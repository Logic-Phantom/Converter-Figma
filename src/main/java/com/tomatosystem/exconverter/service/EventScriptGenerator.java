package com.tomatosystem.exconverter.service;

import com.tomatosystem.exconverter.model.UiIr;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ES5 event-handler skeleton for the listeners the CLX generator wired (v2.2 backend binding): button clicks that
 * send the bound submission, the body load that runs the first query, the grid selection that feeds the detail
 * query, and submit-done handlers that load REST DTO responses into the DataSet/DataMap when the backend does not
 * answer in eXBuilder's own {"dsList":[...]} protocol. Runtime calls used: Submission.send/xhr, DataSet.build/
 * getRowCount, DataMap.build/clear/getValue, Grid.getSelectedRow, Row.getValue (all in exbuilder/runtime/cleopatra.js).
 */
public final class EventScriptGenerator {
	private EventScriptGenerator() { }

	public static String render(UiIr ir) {
		if (ir == null || ir.getHandlers().isEmpty()) return "";
		StringBuilder js = new StringBuilder();
		js.append("\n\n/* ==============================================================\n");
		js.append(" *  백엔드 연동 스켈레톤 (Figma → CLX 변환기 v2.2 자동 생성)\n");
		if (!ir.getApiSource().isEmpty()) js.append(" *  OpenAPI: ").append(ir.getApiSource()).append('\n');
		for (UiIr.Submission s : ir.getSubmissions()) {
			js.append(" *  - ").append(s.getId()).append(": ").append(s.getMethod().toUpperCase(Locale.ROOT)).append(' ').append(s.getAction());
			if (!s.getRequestDataId().isEmpty()) js.append("  요청 ").append(s.getRequestDataId());
			if (!s.getResponseDataId().isEmpty()) js.append(" → 응답 ").append(s.getResponseDataId());
			if (!s.getResponsePath().isEmpty()) js.append(" (").append(s.getResponsePath()).append(')');
			if (!s.getSummary().isEmpty()) js.append("  ").append(s.getSummary());
			js.append('\n');
		}
		js.append(" *  응답이 eXBuilder 프로토콜({\"dsList\":[...]})이 아닌 REST DTO 형식이면 submit-done 에서 경로로 적재한다.\n");
		js.append(" *  TODO 표시된 곳(필수값 검사, 메시지, 페이징 파라미터)은 업무에 맞게 채운다.\n");
		js.append(" * ============================================================== */\n");
		boolean helpers = false;
		for (String event : new String[] { "load", "click", "save", "delete", "selection-change", "submit-done" }) {
			for (UiIr.Handler handler : ir.getHandlers()) {
				if (!event.equals(handler.getEvent())) continue;
				helpers |= append(js, ir, handler);
			}
		}
		if (helpers) appendHelpers(js);
		return js.toString();
	}

	/** @return true when the handler uses the shared helper functions */
	private static boolean append(StringBuilder js, UiIr ir, UiIr.Handler h) {
		UiIr.Submission submission = find(ir, h.getSubmissionId());
		String event = h.getEvent();
		String role = h.getRole();
		boolean helpers = false;
		js.append('\n');
		if ("load".equals(event)) {
			comment(js, "화면 로드 시 호출. 초기 목록을 조회한다.", submission == null ? "" : "(" + submission.getId() + " 전송; 조회조건이 필수라면 이 호출을 지우세요)");
			js.append("function ").append(h.getFunction()).append("(e){\n");
			js.append("\tapp.lookup(\"").append(h.getSubmissionId()).append("\").send();\n}\n");
			return false;
		}
		if ("click".equals(event) || "save".equals(event) || "delete".equals(event)) {
			String where = "click".equals(event)
				? "\"" + h.getCaption() + "\" 버튼" + (h.getControlId().isEmpty() ? "" : "(" + h.getControlId() + ")") + "에서 click 이벤트 발생 시 호출."
				: "udcComGridCudBtns(" + h.getControlId() + ")의 \"" + h.getCaption() + "\" 버튼이 " + event + " 이벤트를 보낼 때 호출" + ("delete".equals(event) ? " (ignoreDefaultDeleteAction=true: 행은 서버 삭제 후 재조회로 사라짐)." : ".");
			if ("reset".equals(role)) {
				UiIr.Region search = ir.firstRegion(UiIr.SEARCH);
				String dataMap = search == null ? "dmSearch" : search.getDataId();
				comment(js, where, "조회조건(" + dataMap + ")을 초기화한다.");
				js.append("function ").append(h.getFunction()).append("(e){\n");
				js.append("\tapp.lookup(\"").append(dataMap).append("\").clear();\n}\n");
				return false;
			}
			if (submission == null) return false;
			String detail = submission.getMethod().toUpperCase(Locale.ROOT) + " " + submission.getAction();
			if (UiIr.Submission.ROLE_SEARCH.equals(role)) comment(js, where, "조회조건(" + orNone(submission.getRequestDataId()) + ")을 " + detail + " 로 보내 목록(" + orNone(submission.getResponseDataId()) + ")을 조회한다.");
			else if (UiIr.Submission.ROLE_SAVE.equals(role)) comment(js, where, orNone(submission.getRequestDataId()) + " 의 값을 " + detail + " 로 저장한다.");
			else if (UiIr.Submission.ROLE_DELETE.equals(role)) comment(js, where, detail + " 로 삭제한다.");
			else comment(js, where, detail + " 를 호출한다.");
			js.append("function ").append(h.getFunction()).append("(e){\n");
			js.append("\tvar ").append("click".equals(event) ? "button" : "udc").append(" = e.control;\n");
			js.append("\tvar submission = app.lookup(\"").append(submission.getId()).append("\");\n");
			if (UiIr.Submission.ROLE_SAVE.equals(role)) {
				js.append("\t// TODO 필수값 검사 (예: if (!app.lookup(\"").append(orNone(submission.getRequestDataId())).append("\").getValue(\"컬럼\")) { alert(\"필수 항목을 입력하세요.\"); return; })\n");
			}
			if (UiIr.Submission.ROLE_DELETE.equals(role)) {
				String gridId = gridControlFor(ir, submission.getRequestDataId());
				if (!gridId.isEmpty()) js.append("\tif (!app.lookup(\"").append(gridId).append("\").getSelectedRow()) { alert(\"삭제할 행을 선택하세요.\"); return; }\n");
				js.append("\tif (!confirm(\"삭제하시겠습니까?\")) return;\n");
			}
			helpers |= pathParams(js, submission, requestGetter(ir, submission));
			js.append("\tsubmission.send();\n}\n");
			return helpers;
		}
		if ("selection-change".equals(event)) {
			comment(js, "목록(" + h.getControlId() + ")의 행을 선택하면 호출.", "선택 행의 키로 상세(" + h.getSubmissionId() + ")를 조회한다.");
			js.append("function ").append(h.getFunction()).append("(e){\n");
			js.append("\tvar grid = e.control;\n");
			js.append("\tvar row = grid.getSelectedRow();\n");
			js.append("\tif (!row) return;\n");
			if (submission == null) { js.append("}\n"); return false; }
			js.append("\tvar submission = app.lookup(\"").append(submission.getId()).append("\");\n");
			helpers |= pathParams(js, submission, "function(name){ return row.getValue(name); }");
			if (!submission.getAction().contains("{")) {
				js.append("\t// TODO 키 컬럼을 조회조건으로 전달 (예: app.lookup(\"").append(orNone(submission.getResponseDataId())).append("\").setValue(\"id\", row.getValue(\"id\")))\n");
			}
			js.append("\tsubmission.send();\n}\n");
			return helpers;
		}
		if ("submit-done".equals(event)) {
			if (submission == null) return false;
			String path = submission.getResponsePath();
			if (UiIr.Submission.ROLE_SEARCH.equals(role) && !submission.getResponseDataId().isEmpty()) {
				comment(js, submission.getId() + " 응답 도착 시 호출.", "프로토콜 형식({\"" + submission.getResponseDataId() + "\":[...]})이면 이미 적재되어 있고, REST DTO 응답이면 경로 \"" + path + "\" 의 배열을 적재한다.");
				js.append("function ").append(h.getFunction()).append("(e){\n");
				js.append("\tvar submission = e.control;\n");
				js.append("\tvar dataSet = app.lookup(\"").append(submission.getResponseDataId()).append("\");\n");
				js.append("\tif (dataSet.getRowCount() > 0) return;\n");
				js.append("\tvar rows = pickPath(parseResponse(submission), \"").append(path).append("\");\n");
				js.append("\tif (rows instanceof Array) dataSet.build(rows);\n}\n");
				return true;
			}
			if (UiIr.Submission.ROLE_DETAIL.equals(role) && !submission.getResponseDataId().isEmpty()) {
				comment(js, submission.getId() + " 응답 도착 시 호출.", "REST DTO 응답이면 경로 \"" + path + "\" 의 객체를 " + submission.getResponseDataId() + " 에 적재한다.");
				js.append("function ").append(h.getFunction()).append("(e){\n");
				js.append("\tvar submission = e.control;\n");
				js.append("\tvar data = pickPath(parseResponse(submission), \"").append(path).append("\");\n");
				js.append("\tif (data && !(data instanceof Array)) app.lookup(\"").append(submission.getResponseDataId()).append("\").build(data);\n}\n");
				return true;
			}
			if (UiIr.Submission.ROLE_SAVE.equals(role) || UiIr.Submission.ROLE_DELETE.equals(role)) {
				String message = UiIr.Submission.ROLE_SAVE.equals(role) ? "저장되었습니다." : "삭제되었습니다.";
				UiIr.Submission list = firstOfRole(ir, UiIr.Submission.ROLE_SEARCH);
				comment(js, submission.getId() + " 응답 도착 시 호출.", "안내 후 목록을 다시 조회한다.");
				js.append("function ").append(h.getFunction()).append("(e){\n");
				js.append("\talert(\"").append(message).append("\"); // TODO 공통 메시지 처리로 교체\n");
				if (list != null) js.append("\tapp.lookup(\"").append(list.getId()).append("\").send();\n");
				js.append("}\n");
				return false;
			}
			comment(js, submission.getId() + " 응답 도착 시 호출.", "TODO 응답 처리");
			js.append("function ").append(h.getFunction()).append("(e){\n");
			js.append("\tvar submission = e.control;\n");
			js.append("\tvar data = parseResponse(submission);\n}\n");
			return true;
		}
		return false;
	}

	/** submission.action with {id}-style path parameters filled from the request data or the selected row. */
	private static boolean pathParams(StringBuilder js, UiIr.Submission submission, String getter) {
		if (!submission.getAction().contains("{")) return false;
		js.append("\tsubmission.action = replacePathParams(\"").append(submission.getAction()).append("\", ").append(getter).append(");\n");
		return true;
	}

	private static String gridControlFor(UiIr ir, String dataId) {
		for (UiIr.Region grid : ir.regionsOf(UiIr.GRID)) { if (dataId.equals(grid.getDataId())) return grid.getControlId(); }
		return "";
	}

	private static String requestGetter(UiIr ir, UiIr.Submission submission) {
		String dataId = submission.getRequestDataId();
		if (dataId.isEmpty()) return "function(name){ return null; /* TODO 경로 파라미터 값 */ }";
		for (UiIr.Region grid : ir.regionsOf(UiIr.GRID)) {
			if (!dataId.equals(grid.getDataId())) continue;
			if (!grid.getControlId().isEmpty()) return "function(name){ var row = app.lookup(\"" + grid.getControlId() + "\").getSelectedRow(); return row ? row.getValue(name) : null; }";
			return "function(name){ var row = app.lookup(\"" + dataId + "\").getRow(0); return row ? row.getValue(name) : null; /* TODO 선택 행으로 교체 */ }";
		}
		return "function(name){ return app.lookup(\"" + dataId + "\").getValue(name); }";
	}

	private static void appendHelpers(StringBuilder js) {
		js.append("\n/*\n * 응답 본문을 JSON 으로 읽는다 (REST DTO 응답용; 프로토콜 응답은 런타임이 이미 적재).\n */\n");
		js.append("function parseResponse(submission){\n\ttry { return JSON.parse(submission.xhr.responseText); } catch (ex) { return null; }\n}\n");
		js.append("\n/*\n * \"data.list\" 같은 점 경로로 값을 꺼낸다. 경로가 비어 있으면 본문 자체.\n */\n");
		js.append("function pickPath(obj, path){\n\tif (!obj || !path) return obj;\n\tvar parts = path.split(\".\");\n\tvar cur = obj;\n\tfor (var i = 0; i < parts.length; i++) {\n\t\tif (cur == null) return null;\n\t\tcur = cur[parts[i]];\n\t}\n\treturn cur;\n}\n");
		js.append("\n/*\n * \"/api/users/{id}\" 의 경로 파라미터를 채운다.\n */\n");
		js.append("function replacePathParams(action, valueOf){\n\treturn action.replace(/\\{([^}]+)\\}/g, function(m, name){ var v = valueOf(name); return v == null ? \"\" : encodeURIComponent(v); });\n}\n");
	}

	private static void comment(StringBuilder js, String first, String second) {
		js.append("/*\n * ").append(first).append('\n');
		if (second != null && !second.isEmpty()) js.append(" * ").append(second).append('\n');
		js.append(" */\n");
	}

	private static UiIr.Submission find(UiIr ir, String id) {
		for (UiIr.Submission s : ir.getSubmissions()) { if (s.getId().equals(id)) return s; }
		return null;
	}

	private static UiIr.Submission firstOfRole(UiIr ir, String role) {
		for (UiIr.Submission s : ir.getSubmissions()) { if (role.equals(s.getRole())) return s; }
		return null;
	}

	private static String orNone(String id) { return id == null || id.isEmpty() ? "-" : id; }

	/** Names of the functions the script defines, for tests. */
	public static List<String> functions(UiIr ir) {
		List<String> names = new ArrayList<String>();
		for (UiIr.Handler h : ir.getHandlers()) names.add(h.getFunction());
		return names;
	}
}
