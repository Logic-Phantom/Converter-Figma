package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic structural repair of UI-IR produced by small local vision models.
 * The model decides meaning; these rules undo its typical table-reading mistakes
 * before the CLX compiler runs. Every repair is recorded in {@link UiIr#getWarnings()}.
 *
 * <ol>
 *   <li>Row-split grids: one grid region per data row (plus a header-only region) → one grid.</li>
 *   <li>Header row read as a form: form labels immediately followed by a grid whose headers are
 *       cell values (FNM, ACNO____, #) → one grid with the labels as headers.</li>
 *   <li>Meaningless uniform column widths → unknown (compiler estimates).</li>
 *   <li>A textarea "title" copied from the preceding grid → removed.</li>
 *   <li>(rule 5 is mergeValuesReadAsLabels)</li>
 *   <li>A sectionTitle directly above a grid/form/textarea → folded into that region's title.</li>
 *   <li>A "총 N건" row counter in a grid title → removed (udcComGridTitle draws it).</li>
 * </ol>
 */
public final class UiIrNormalizer {
	private static final String CODE_LIKE = "[A-Z][A-Z0-9_]*[-_]*|[#_\\-\\s]+|\\d+";

	private UiIrNormalizer() { }

	public static UiIr normalize(UiIr ir) {
		List<UiIr.Region> regions = ir.getRegions();
		mergeRowSplitGrids(regions, ir.getWarnings());
		mergeHeaderFormIntoGrid(regions, ir.getWarnings());
		for (int i = 0; i < regions.size(); i++) {
			UiIr.Region region = regions.get(i);
			if (UiIr.SEARCH.equals(region.getType()) || UiIr.FORM.equals(region.getType())) mergeValuesReadAsLabels(region, ir.getWarnings());
			if (UiIr.GRID.equals(region.getType())) resetUniformWidths(region);
			if (UiIr.TEXTAREA.equals(region.getType()) && i > 0 && UiIr.GRID.equals(regions.get(i - 1).getType())) {
				UiIr.Region grid = regions.get(i - 1);
				if (region.getTitle().equals(grid.getTitle()) || headers(grid).contains(region.getTitle())) region.setTitle("");
			}
		}
		stripRowCountFromTitles(regions, ir.getWarnings());
		absorbSectionTitles(regions, ir.getWarnings());
		return ir;
	}

	// ------------------------------------------------------------------ rules 6, 7

	/** "사용자 목록  총 0건": the counter is drawn by udcComGridTitle itself (총건수 N건), so it is not part of the title. */
	private static final String ROW_COUNT_SUFFIX = "\\s*[\\[(]?\\s*총\\s*(건수)?\\s*[0-9,]+\\s*건\\s*[\\])]?\\s*$";

	static void stripRowCountFromTitles(List<UiIr.Region> regions, List<String> warnings) {
		for (UiIr.Region region : regions) {
			if (!UiIr.GRID.equals(region.getType())) continue;
			String title = region.getTitle();
			String stripped = title.replaceAll(ROW_COUNT_SUFFIX, "").trim();
			if (stripped.equals(title.trim())) continue;
			region.setTitle(stripped);
			warnings.add("보정: 그리드 제목의 건수 표시 제거 (" + title.trim() + " → " + stripped + ")");
		}
	}

	/**
	 * A sectionTitle directly above a grid, form or textarea is that region's heading. Emitting both makes the CLX
	 * show the heading twice (content-title-box output + udcComGridTitle), so fold it into the region's title.
	 */
	static void absorbSectionTitles(List<UiIr.Region> regions, List<String> warnings) {
		for (int i = 0; i + 1 < regions.size(); i++) {
			UiIr.Region section = regions.get(i);
			UiIr.Region next = regions.get(i + 1);
			if (!UiIr.SECTION_TITLE.equals(section.getType())) continue;
			String type = next.getType();
			if (!(UiIr.GRID.equals(type) || UiIr.FORM.equals(type) || UiIr.TEXTAREA.equals(type))) continue;
			if (!section.getSide().isEmpty() && !section.getSide().equals(next.getSide())) continue;
			String text = firstNonBlank(section.getText(), section.getTitle()).replaceAll(ROW_COUNT_SUFFIX, "").trim();
			String title = next.getTitle();
			if (title.isEmpty()) next.setTitle(text);
			else if (!compact(title).equals(compact(text))) continue;
			regions.remove(i);
			i--;
			warnings.add("보정: 섹션 제목 '" + text + "' 을 바로 아래 " + type + " 제목으로 통합");
		}
	}

	private static String compact(String text) { return text.replaceAll("\\s+", ""); }

	// ------------------------------------------------------------------ rule 1

	static void mergeRowSplitGrids(List<UiIr.Region> regions, List<String> warnings) {
		for (int start = 0; start < regions.size(); start++) {
			if (!UiIr.GRID.equals(regions.get(start).getType())) continue;
			int end = start + 1;
			while (end < regions.size() && UiIr.GRID.equals(regions.get(end).getType()) && related(regions.get(start), regions.get(end))) end++;
			if (end - start < 2) continue;
			List<UiIr.Region> run = new ArrayList<UiIr.Region>(regions.subList(start, end));
			if (!hasRowSplitSignature(run)) continue;
			UiIr.Region merged = mergeRun(run);
			regions.subList(start, end).clear();
			regions.add(start, merged);
			warnings.add("보정: 행 단위로 쪼개진 그리드 " + run.size() + "개를 1개로 병합 (" + merged.getColumns().size() + "컬럼)");
		}
	}

	private static boolean related(UiIr.Region a, UiIr.Region b) {
		boolean titlesCompatible = a.getTitle().isEmpty() || b.getTitle().isEmpty() || a.getTitle().equals(b.getTitle());
		List<String> common = new ArrayList<String>(headers(a));
		common.retainAll(headers(b));
		return titlesCompatible && common.size() >= 2 && a.getSide().equals(b.getSide()) && a.isInTab() == b.isInTab();
	}

	/** Header-only region, or a column whose "header" is really that row's value. */
	private static boolean hasRowSplitSignature(List<UiIr.Region> run) {
		Map<String, Integer> counts = headerCounts(run);
		for (UiIr.Region grid : run) {
			boolean headerOnly = true;
			for (UiIr.Column column : grid.getColumns()) {
				if (!column.getCellText().isEmpty()) headerOnly = false;
				if (counts.get(column.getHeader()) == 1 && column.getHeader().equals(column.getCellText())) return true;
			}
			if (headerOnly) return true;
		}
		return false;
	}

	private static UiIr.Region mergeRun(List<UiIr.Region> run) {
		Map<String, Integer> counts = headerCounts(run);
		List<String> canonical = new ArrayList<String>();
		for (UiIr.Region grid : run) {
			for (String header : headers(grid)) { if (counts.get(header) >= 2 && !canonical.contains(header)) canonical.add(header); }
		}
		List<List<String>> rows = new ArrayList<List<String>>();
		for (UiIr.Region grid : run) {
			List<String> values = new ArrayList<String>();
			boolean any = false;
			for (UiIr.Column column : grid.getColumns()) { values.add(column.getCellText().trim()); any |= !column.getCellText().trim().isEmpty(); }
			if (any) rows.add(values);
		}
		boolean rowIndex = !rows.isEmpty();
		for (int r = 0; r < rows.size() && rowIndex; r++) rowIndex = !rows.get(r).isEmpty() && rows.get(r).get(0).matches("\\d+");
		if (rowIndex) { for (List<String> row : rows) row.remove(0); }

		UiIr.Region merged = new UiIr.Region(UiIr.GRID);
		merged.setSide(run.get(0).getSide());
		merged.setInTab(run.get(0).isInTab());
		for (UiIr.Region grid : run) {
			if (grid.isPaging()) merged.setPaging(true);
			for (String button : grid.getButtons()) { if (!merged.getButtons().contains(button)) merged.getButtons().add(button); }
		}
		for (UiIr.Region grid : run) { if (!grid.getTitle().isEmpty() && !canonical.contains(grid.getTitle())) { merged.setTitle(grid.getTitle()); break; } }
		if (rowIndex) merged.getColumns().add(new UiIr.Column("", "rowindex", 0, "1"));
		for (int c = 0; c < canonical.size(); c++) {
			String header = canonical.get(c);
			List<String> values = new ArrayList<String>();
			for (List<String> row : rows) {
				// Rows that list exactly one value per canonical header are positional; otherwise nothing is guessed.
				if (row.size() == canonical.size()) values.add(row.get(c));
			}
			UiIr.Column original = firstColumn(run, header);
			String editor = original.getEditor();
			if (isRepeatedAction(values)) editor = "button";
			else if ("rowindex".equals(editor) && !rowIndex) editor = "output";
			merged.getColumns().add(new UiIr.Column(header, editor, maxWidth(run, header), values.isEmpty() ? original.getCellText() : values.get(0)));
		}
		return merged;
	}

	/** Same short label in every row (e.g. "실행") is a button column. */
	private static boolean isRepeatedAction(List<String> values) {
		if (values.size() < 2 || values.get(0).isEmpty() || values.get(0).length() > 6 || values.get(0).matches(CODE_LIKE)) return false;
		for (String value : values) { if (!value.equals(values.get(0))) return false; }
		return true;
	}

	// ------------------------------------------------------------------ rule 2

	static void mergeHeaderFormIntoGrid(List<UiIr.Region> regions, List<String> warnings) {
		for (int i = 0; i + 1 < regions.size(); i++) {
			UiIr.Region form = regions.get(i);
			UiIr.Region grid = regions.get(i + 1);
			if (!(UiIr.FORM.equals(form.getType()) || UiIr.SEARCH.equals(form.getType())) || !UiIr.GRID.equals(grid.getType())) continue;
			if (UiIr.SEARCH.equals(form.getType()) && !form.getButtons().isEmpty()) continue;
			if (!form.getSide().equals(grid.getSide()) || form.isInTab() != grid.isInTab()) continue;
			if (!headersLookLikeCellValues(grid) || form.getFields().size() + 1 < grid.getColumns().size()) continue;
			UiIr.Region merged = alignLabelsWithCells(form, grid);
			regions.set(i, merged);
			regions.remove(i + 1);
			warnings.add("보정: 그리드 헤더 행으로 보이는 " + form.getType() + "(라벨 " + form.getFields().size() + "개)와 데이터 행 그리드를 1개 그리드로 병합 (" + merged.getColumns().size() + "컬럼)");
		}
	}

	private static boolean headersLookLikeCellValues(UiIr.Region grid) {
		int valueLike = 0;
		for (UiIr.Column column : grid.getColumns()) {
			String header = column.getHeader().trim();
			if (header.matches(CODE_LIKE) || (!header.isEmpty() && header.equals(column.getCellText().trim()))) valueLike++;
		}
		return !grid.getColumns().isEmpty() && valueLike >= Math.ceil(grid.getColumns().size() * 0.6);
	}

	/** Order-preserving alignment (weighted LCS) of header labels to data cells. */
	private static UiIr.Region alignLabelsWithCells(UiIr.Region form, UiIr.Region grid) {
		List<UiIr.Field> labels = form.getFields();
		List<UiIr.Column> cells = grid.getColumns();
		int n = labels.size();
		int m = cells.size();
		int[][] score = new int[n + 1][m + 1];
		for (int a = n - 1; a >= 0; a--) {
			for (int b = m - 1; b >= 0; b--) {
				int match = matchScore(labels.get(a).getLabel(), cellValue(cells.get(b)));
				score[a][b] = Math.max(Math.max(score[a + 1][b], score[a][b + 1]), match > 0 ? match + score[a + 1][b + 1] : 0);
			}
		}
		UiIr.Region merged = new UiIr.Region(UiIr.GRID);
		merged.setSide(grid.getSide());
		merged.setInTab(grid.isInTab());
		merged.setPaging(grid.isPaging());
		merged.getButtons().addAll(grid.getButtons());
		String title = firstNonBlank(form.getTitle(), grid.getTitle());
		for (UiIr.Field field : labels) { if (field.getLabel().equals(title)) { title = ""; break; } }
		merged.setTitle(title);
		int a = 0;
		int b = 0;
		while (a < n || b < m) {
			if (a < n && b < m) {
				// On ties leave the earlier label empty: a misread duplicate ("성명, 성명") usually stands for a column whose cell is blank.
				if (score[a][b] == score[a + 1][b]) { merged.getColumns().add(unmatchedLabel(labels, a, cells, b)); a++; continue; }
				int match = matchScore(labels.get(a).getLabel(), cellValue(cells.get(b)));
				if (match > 0 && score[a][b] == match + score[a + 1][b + 1]) {
					merged.getColumns().add(column(labels.get(a).getLabel(), cells.get(b)));
					a++; b++;
					continue;
				}
				merged.getColumns().add(cells.get(b)); b++;
				continue;
			}
			if (a < n) { merged.getColumns().add(unmatchedLabel(labels, a, cells, b)); a++; }
			else { merged.getColumns().add(cells.get(b)); b++; }
		}
		return merged;
	}

	/** 2 = semantically the same column, 1 = compatible placeholder pairing, 0 = no pairing. */
	private static int matchScore(String label, String cell) {
		if (cell.matches("#|\\d+")) return ColumnNames.isSequenceLabel(label) ? 2 : 0;
		if (cell.matches("[_\\-\\s]+")) return label.matches(".*(연락처|전화|휴대|핸드폰|팩스|우편).*") ? 2 : 0;
		String code = ColumnNames.codeOf(cell);
		if (code.isEmpty()) return 0;
		for (String candidate : ColumnNames.codesFor(label)) {
			if (code.equals(candidate) || (candidate.length() >= 2 && (code.endsWith(candidate) || candidate.endsWith(code)))) return 2;
		}
		return ColumnNames.isSequenceLabel(label) ? 0 : 1;
	}

	private static String cellValue(UiIr.Column column) {
		return column.getCellText().trim().isEmpty() ? column.getHeader().trim() : column.getCellText().trim();
	}

	private static UiIr.Column column(String label, UiIr.Column cell) {
		String value = cellValue(cell);
		if (value.matches("#|\\d+")) {
			// Only sequence-like labels reach here; OCR typos such as "변호" become "번호".
			String header = label.trim().matches("(?i)번호|순번|no\\.?|#") ? label.trim() : "번호";
			return new UiIr.Column(header, "rowindex", cell.getWidth(), value);
		}
		String editor = cell.getEditor();
		if (value.matches("[A-Z][A-Z0-9]*[_\\-]{2,}|[_\\-\\s]{3,}")) editor = "maskeditor";
		return new UiIr.Column(label, editor, cell.getWidth(), value);
	}

	/** A header with an empty data cell. A leading one before the row number is the usual row-check column. */
	private static UiIr.Column unmatchedLabel(List<UiIr.Field> labels, int a, List<UiIr.Column> cells, int b) {
		boolean beforeRowNumber = b < cells.size() && cellValue(cells.get(b)).matches("#|\\d+") && a + 1 < labels.size();
		return new UiIr.Column(labels.get(a).getLabel(), beforeRowNumber && a == 0 ? "checkbox" : "output", 0, "");
	}

	// ------------------------------------------------------------------ rule 5

	private static final String SURNAMES = "김이박최정강조윤장임한오서신권황안송전홍유고문양손배백허남심노하곽성차주우구민류나진지엄채원천방공현함변염여추도소석선설마길연위표명기반왕금옥육인맹제모탁국어은편용예경봉사부";
	private static final String[] COMPOUND_SURNAMES = { "남궁", "황보", "제갈", "선우", "독고", "사공", "서문" };
	/** Words that open with a surname character but are labels, not names. */
	private static final String LABEL_WORDS = ".*(명|번호|일자|여부|코드|구분|유형|상태|기간|주소|권한|등급|부서|직급|직위|분류|항목|금액|내용|제목|사용자|담당자|작성자|등록자|신청자|관리자|처리자|승인자|요청자|아이디|비밀번호|메일|연락처|전화|휴대폰)$";

	/**
	 * Text typed inside an input box read as its own label:
	 * [성명(value ""), 김길동(value ""), 이메일] → [성명(value "김길동"), 이메일].
	 * Only merges when the previous field is still empty and the text fits what that field holds.
	 */
	static void mergeValuesReadAsLabels(UiIr.Region region, List<String> warnings) {
		List<UiIr.Field> fields = region.getFields();
		for (int i = 1; i < fields.size(); i++) {
			UiIr.Field previous = fields.get(i - 1);
			UiIr.Field candidate = fields.get(i);
			if (!previous.getValue().trim().isEmpty() || !candidate.getValue().trim().isEmpty()) continue;
			if ("output".equals(previous.getComponent()) || "checkbox".equals(previous.getComponent()) || "radiobutton".equals(previous.getComponent())) continue;
			String text = candidate.getLabel().trim();
			if (!isValueFor(previous.getLabel().trim(), text)) continue;
			UiIr.Field repaired = new UiIr.Field(previous.getLabel(), previous.getComponent(), previous.isRequired(), text);
			repaired.getOptions().addAll(previous.getOptions());
			fields.set(i - 1, repaired);
			fields.remove(i);
			warnings.add("보정: '" + previous.getLabel() + "' 입력칸의 값 '" + text + "' 이(가) 라벨로 인식되어 필드에서 제거");
			i--;
		}
	}

	static boolean isValueFor(String label, String text) {
		if (text.isEmpty() || text.equals(label)) return false;
		// Shapes that are never labels.
		if (text.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) return true;
		if (text.matches("\\d{2,4}-\\d{3,4}-\\d{4}")) return true;
		if (text.matches("\\d{4}[-./]\\d{1,2}[-./]\\d{1,2}")) return true;
		if (text.matches("[\\d,.]+") || text.matches("(?i)https?://\\S+")) return true;
		// A person's name, only right after a label that asks for one.
		boolean personLabel = label.matches(".*(성명|이름|성함|담당자|작성자|등록자|신청자|사용자명|고객명|사원명|직원명|대표자).*");
		return personLabel && isKoreanPersonName(text);
	}

	private static boolean isKoreanPersonName(String text) {
		if (!text.matches("[가-힣]{3,4}") || text.matches(LABEL_WORDS) || !ColumnNames.codesFor(text).isEmpty()) return false;
		if (text.length() == 4) {
			for (String surname : COMPOUND_SURNAMES) { if (text.startsWith(surname)) return true; }
			return false;
		}
		return SURNAMES.indexOf(text.charAt(0)) >= 0;
	}

	// ------------------------------------------------------------------ rule 3 / helpers

	private static void resetUniformWidths(UiIr.Region grid) {
		List<UiIr.Column> columns = grid.getColumns();
		int width = -1;
		int known = 0;
		for (UiIr.Column column : columns) {
			if (column.getWidth() <= 0) continue;
			if (width >= 0 && column.getWidth() != width) return;
			width = column.getWidth();
			known++;
		}
		// Identical tiny widths (e.g. every column "20") carry no layout information.
		if (known < 2 || width > 40) return;
		for (int i = 0; i < columns.size(); i++) {
			UiIr.Column c = columns.get(i);
			columns.set(i, new UiIr.Column(c.getHeader(), c.getEditor(), 0, c.getCellText()));
		}
	}

	private static List<String> headers(UiIr.Region grid) {
		List<String> result = new ArrayList<String>();
		for (UiIr.Column column : grid.getColumns()) result.add(column.getHeader());
		return result;
	}

	private static Map<String, Integer> headerCounts(List<UiIr.Region> run) {
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
		for (UiIr.Region grid : run) {
			for (String header : new java.util.LinkedHashSet<String>(headers(grid))) counts.put(header, counts.containsKey(header) ? counts.get(header) + 1 : 1);
		}
		return counts;
	}

	private static UiIr.Column firstColumn(List<UiIr.Region> run, String header) {
		for (UiIr.Region grid : run) { for (UiIr.Column column : grid.getColumns()) { if (column.getHeader().equals(header)) return column; } }
		return new UiIr.Column(header, "output", 0, "");
	}

	private static int maxWidth(List<UiIr.Region> run, String header) {
		int width = 0;
		for (UiIr.Region grid : run) { for (UiIr.Column column : grid.getColumns()) { if (column.getHeader().equals(header)) width = Math.max(width, column.getWidth()); } }
		return width;
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) { if (value != null && !value.trim().isEmpty()) return value.trim(); }
		return "";
	}
}
