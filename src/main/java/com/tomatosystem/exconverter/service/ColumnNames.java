package com.tomatosystem.exconverter.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Korean business label ↔ conventional column code dictionary shared by the normalizer and the CLX compiler. */
public final class ColumnNames {
	private static final Map<String, String[]> CODES = new LinkedHashMap<String, String[]>();
	static {
		put("주민등록번호", "RRNO"); put("사업자등록번호", "BZNO"); put("법인등록번호", "CRNO"); put("등록번호", "BZNO", "RRNO", "CRNO", "REG_NO");
		put("계좌번호", "ACNO"); put("카드번호", "CDNO"); put("전화번호", "TELNO"); put("휴대폰", "MBLNO"); put("핸드폰", "MBLNO");
		put("연락처", "TELNO"); put("이메일", "EMAIL"); put("성명", "NM", "FNM"); put("이름", "NM", "FNM"); put("성별", "SEX", "GENDER");
		put("생년월일", "BRDT"); put("주소", "ADDR"); put("우편번호", "ZIP"); put("부서", "DEPT_NM"); put("직급", "JBGD_NM");
		put("등록일", "REG_DT"); put("수정일", "MDFCN_DT"); put("시작일", "BGNG_YMD"); put("종료일", "END_YMD"); put("일자", "YMD");
		put("사용여부", "USE_YN"); put("비고", "RMRK"); put("설명", "EXPLN"); put("코드", "CD"); put("명칭", "NM"); put("금액", "AMT");
		put("수량", "QTY"); put("상태", "STTS"); put("구분", "SE"); put("제목", "TTL"); put("내용", "CN"); put("아이디", "USER_ID");
		put("동작", "ACTION"); put("함수", "FUNC_NM");
	}

	private ColumnNames() { }

	private static void put(String label, String... codes) { CODES.put(label, codes); }

	/** Preferred code for a header, or "" when the dictionary has no entry. */
	public static String nameFor(String header) {
		List<String> codes = codesFor(header);
		return codes.isEmpty() ? "" : codes.get(0);
	}

	/** All plausible codes for a label (first dictionary key contained in the label wins). */
	public static List<String> codesFor(String label) {
		String compact = label == null ? "" : label.replace(" ", "");
		for (Map.Entry<String, String[]> entry : CODES.entrySet()) {
			if (compact.contains(entry.getKey())) return java.util.Arrays.asList(entry.getValue());
		}
		return new ArrayList<String>();
	}

	/** Design-mode grids print the bound column in the data cell, optionally followed by a mask ("ACNO_____"). */
	public static String codeOf(String cellText) {
		String text = cellText == null ? "" : cellText.trim();
		return text.matches("[A-Z][A-Z0-9_]*[-_]*") ? text.replaceAll("[-_]+$", "").toUpperCase(Locale.ROOT) : "";
	}

	public static boolean isSequenceLabel(String label) {
		String t = label == null ? "" : label.trim();
		return t.matches("(?i)(번호|순번|no\\.?|#)") || (t.length() == 2 && (distance(t, "번호") <= 1 || distance(t, "순번") <= 1));
	}

	/** Levenshtein distance; labels are short so the quadratic cost is irrelevant. */
	static int distance(String a, String b) {
		int[] prev = new int[b.length() + 1];
		int[] cur = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) prev[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (int j = 1; j <= b.length(); j++) cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
			int[] t = prev; prev = cur; cur = t;
		}
		return prev[b.length()];
	}
}
