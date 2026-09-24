package com.tomatosystem.figma.api;

import com.tomatosystem.exconverter.service.ColumnNames;
import com.tomatosystem.exconverter.service.ExConverterConfig;
import com.tomatosystem.exconverter.service.ProgressLog;
import com.tomatosystem.figma.ai.AiClient;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pairs Korean screen labels ("예산연도", "주민등록번호") with DTO property names ("budgetYear", "rrn").
 *
 * <p>Deterministic first: the property's description/title containing the label, the label's conventional column
 * codes ({@link ColumnNames}) and a Korean → English keyword table against the property's name tokens. Only labels
 * that stay unmatched are offered to the AI (when a provider is configured), which may pick from the remaining
 * property names only; answers are cached under generated/ai-cache/api-labels.json.
 */
public final class ApiLabelMatcher {
	/** Rule score below which a pairing is not trusted. */
	static final int MIN_SCORE = 40;
	private static final String SYSTEM = String.join("\n",
		"당신은 한국 업무 시스템의 화면 라벨과 REST API DTO 필드를 짝짓는 전문가입니다.",
		"화면 라벨 목록과 후보 필드 목록(이름, 설명)을 받아 각 라벨에 맞는 필드 이름을 고르세요.",
		"후보 목록에 있는 필드 이름만 그대로 사용하고, 맞는 필드가 없으면 그 라벨은 생략하세요. 한 필드는 한 라벨에만 배정합니다.",
		"출력은 아래 JSON 만: {\"map\":{\"라벨\":\"fieldName\", ...}}");

	/** Korean label fragment → English name tokens found in DTO property names. Longer fragments are listed first. */
	private static final String[][] KEYWORDS = {
		{ "주민등록번호", "rrn rrno jumin ssn resident residentno" }, { "사업자등록번호", "bzno brn bizno business" }, { "법인등록번호", "crno corp" },
		{ "등록번호", "regno reg registration" }, { "계좌번호", "acno account acct" }, { "카드번호", "cardno card" }, { "전화번호", "tel telno phone" },
		{ "휴대폰", "mobile mblno hp cell" }, { "핸드폰", "mobile mblno hp cell" }, { "연락처", "tel phone contact mobile" }, { "이메일", "email mail" },
		{ "우편번호", "zip zipcode post postal" }, { "생년월일", "birth brdt birthday bday" }, { "예산연도", "budget bdgt year yr" }, { "회계연도", "fiscal fy year" },
		{ "상병코드", "disease sick dss diag diagnosis cd code" }, { "보건소장", "director dir chief" }, { "위임여부", "delegate dlgt yn" }, { "위임", "delegate dlgt" },
		{ "퇴록여부", "withdraw close out retire yn" }, { "퇴록", "withdraw close out retire" }, { "지원신청서", "apply application aply doc support" },
		{ "전산입력일", "input entry inpt dt date" }, { "입력일", "input entry inpt dt date" }, { "등록일자", "reg regdt regdate created create dt date ymd" },
		{ "등록일", "reg regdt regdate created create dt date ymd" }, { "수정일", "mdfcn upd updated modified update dt date" }, { "삭제일", "del deleted dt date" },
		{ "시작일", "start from begin bgng st dt date ymd" }, { "종료일", "end to fin dt date ymd" }, { "대상자구분", "target tgt subject type se" },
		{ "대상자", "target tgt subject person" }, { "사용여부", "use yn flag" }, { "자격변동", "qlfc qualification eligibility change chg" },
		{ "자격", "qlfc qualification eligibility" }, { "변동", "change chg" }, { "사용자명", "user name nm" }, { "사용자", "user usr" },
		{ "담당자", "manager charger mngr pic" }, { "작성자", "writer author creator reg" }, { "신청자", "applicant requester" }, { "고객", "customer cust" },
		{ "회원", "member mbr" }, { "부서", "dept department" }, { "직급", "grade position jbgd rank" }, { "직위", "position title" },
		{ "성명", "name nm fnm fullname" }, { "이름", "name nm fnm" }, { "성별", "gender sex" }, { "주소", "addr address" }, { "아이디", "id userid login" },
		{ "비밀번호", "password pwd pw" }, { "제목", "title ttl subject" }, { "내용", "content cn body text" }, { "비고", "remark rmrk note memo" },
		{ "설명", "desc description expln" }, { "메모", "memo note" }, { "금액", "amt amount price" }, { "단가", "price unitprice" }, { "수량", "qty quantity cnt count" },
		{ "건수", "cnt count total" }, { "순번", "seq sn no idx index" }, { "번호", "no num number seq" }, { "상태", "status stts state" }, { "구분", "type se gubun div cls kind" },
		{ "유형", "type kind" }, { "종류", "type kind" }, { "코드", "cd code" }, { "명칭", "name nm" }, { "여부", "yn flag is" }, { "일자", "date dt ymd day" },
		{ "날짜", "date dt ymd day" }, { "일시", "datetime dt time" }, { "시간", "time tm" }, { "연도", "year yr yyyy" }, { "년도", "year yr yyyy" }, { "월", "month mm" },
		{ "시작", "start from begin bgng st" }, { "종료", "end to fin" }, { "기간", "period term" }, { "지역", "area region" }, { "국가", "country nation" },
		{ "회사", "company comp corp" }, { "기관", "org organization inst institution" }, { "사업", "biz business project" }, { "프로젝트", "project prj" },
		{ "파일", "file attach" }, { "첨부", "attach file" }, { "승인", "approve appr approval" }, { "요청", "request req" }, { "신청", "apply aply application request" },
		{ "지원", "support sprt aid" }, { "등록", "reg register" }, { "카드", "card" }, { "보험", "insurance insr" }, { "급여", "benefit pay salary" },
		{ "건강", "health hlth" }, { "성인", "adult" }, { "소아", "child pediatric" }, { "의료", "medical mdcl" }, { "평가", "eval evaluation assess" },
		{ "점수", "score point" }, { "등급", "grade level" }, { "순위", "rank ranking" }, { "관리", "mgmt manage management" }, { "정보", "info information" },
		{ "목록", "list" }, { "상세", "detail dtl" }, { "확인", "confirm check" }, { "페이지", "page" }, { "크기", "size" }, { "언어", "lang language" },
		{ "동작", "action" }, { "함수", "func function" } };

	private final AiClient client;
	private final File cacheFile;
	private final Map<String, String> cache = new LinkedHashMap<String, String>();

	public ApiLabelMatcher(AiClient client) {
		this.client = client;
		this.cacheFile = new File(new File(ExConverterConfig.get("exconverter.generated.root", "generated"), "ai-cache"), "api-labels.json");
		load();
	}

	/** One label ↔ property pairing with the rule that made it. */
	public static final class Match {
		public final String label; public final OpenApiSpec.Property property; public final int score; public final String reason;
		Match(String label, OpenApiSpec.Property property, int score, String reason) { this.label = label; this.property = property; this.score = score; this.reason = reason; }
	}

	/**
	 * Pairs labels with candidates; every candidate is used at most once, best scores first, then the AI for the rest.
	 *
	 * @param scope cache namespace (spec title + operation), so the same label may map differently per API
	 */
	public Map<String, Match> match(List<String> labels, List<OpenApiSpec.Property> candidates, String scope) {
		Map<String, Match> result = new LinkedHashMap<String, Match>();
		List<Match> all = new ArrayList<Match>();
		for (String label : new LinkedHashSet<String>(labels)) {
			for (OpenApiSpec.Property candidate : candidates) {
				int score = score(label, candidate);
				if (score >= MIN_SCORE) all.add(new Match(label, candidate, score, "rule"));
			}
		}
		all.sort((a, b) -> b.score != a.score ? b.score - a.score : a.label.compareTo(b.label));
		Set<String> usedProperties = new LinkedHashSet<String>();
		for (Match m : all) {
			if (result.containsKey(m.label) || usedProperties.contains(m.property.name)) continue;
			result.put(m.label, m);
			usedProperties.add(m.property.name);
		}
		List<String> unresolved = new ArrayList<String>();
		for (String label : new LinkedHashSet<String>(labels)) { if (!result.containsKey(label)) unresolved.add(label); }
		List<OpenApiSpec.Property> remaining = new ArrayList<OpenApiSpec.Property>();
		for (OpenApiSpec.Property c : candidates) { if (!usedProperties.contains(c.name)) remaining.add(c); }
		// Cache (earlier AI answers) before a new call.
		for (java.util.Iterator<String> it = unresolved.iterator(); it.hasNext();) {
			String label = it.next();
			String cached = cache.get(key(scope, label));
			OpenApiSpec.Property property = cached == null ? null : find(remaining, cached);
			if (property == null) continue;
			result.put(label, new Match(label, property, 60, "cache"));
			remaining.remove(property);
			it.remove();
		}
		if (!unresolved.isEmpty() && !remaining.isEmpty() && client != null && client.isEnabled()) {
			Map<String, String> answer = ask(unresolved, remaining);
			boolean changed = false;
			for (String label : unresolved) {
				OpenApiSpec.Property property = find(remaining, answer.get(label));
				if (property == null) continue;
				result.put(label, new Match(label, property, 50, "ai"));
				remaining.remove(property);
				cache.put(key(scope, label), property.name);
				changed = true;
			}
			if (changed) save();
		}
		return result;
	}

	// ------------------------------------------------------------------ rules

	/** 0..100: description containing the label 90+, conventional column code 100, keyword tokens 50..85. */
	public static int score(String label, OpenApiSpec.Property property) {
		String compactLabel = compact(label);
		if (compactLabel.isEmpty()) return 0;
		int best = 0;
		String description = compact(property.description);
		if (!description.isEmpty()) {
			if (description.equals(compactLabel)) best = Math.max(best, 100);
			else if (description.contains(compactLabel) || compactLabel.contains(description) && description.length() >= 2) best = Math.max(best, 90);
		}
		List<String> tokens = tokens(property.name);
		String flat = String.join("", tokens);
		for (String code : ColumnNames.codesFor(label)) {
			String c = code.toLowerCase(Locale.ROOT).replace("_", "");
			if (flat.equals(c)) best = Math.max(best, 100);
			else if (flat.endsWith(c) || flat.startsWith(c)) best = Math.max(best, 70);
		}
		// Latin labels ("NO", "ID", "Email") compare directly.
		String latin = compactLabel.toLowerCase(Locale.ROOT);
		if (latin.matches("[a-z0-9_]+")) {
			if (flat.equals(latin.replace("_", ""))) best = Math.max(best, 100);
			else if (flat.contains(latin.replace("_", "")) && latin.length() >= 3) best = Math.max(best, 60);
		}
		// Keyword table: every fragment of the label should be represented in the property name.
		List<List<String>> fragments = fragments(compactLabel);
		if (!fragments.isEmpty()) {
			int hit = 0;
			for (List<String> alternatives : fragments) {
				boolean found = false;
				for (String alt : alternatives) { for (String token : tokens) { if (token.equals(alt) || token.startsWith(alt) && alt.length() >= 3) { found = true; break; } } if (found) break; }
				if (found) hit++;
			}
			// Coverage: how many of the property's own tokens the label explains (regDate 2/2 beats otherYearRegYn 1/4 for 등록일).
			Set<String> alternatives = new LinkedHashSet<String>();
			for (List<String> f : fragments) alternatives.addAll(f);
			int covered = 0;
			for (String token : tokens) { for (String alt : alternatives) { if (token.equals(alt) || alt.length() >= 3 && token.startsWith(alt)) { covered++; break; } } }
			double coverage = tokens.isEmpty() ? 0 : covered / (double) tokens.size();
			if (hit == fragments.size()) best = Math.max(best, 55 + Math.min(30, fragments.size() * 15) + (int) Math.round(coverage * 14));
			else if (hit > 0 && hit * 2 >= fragments.size() && tokens.size() <= hit + 1) best = Math.max(best, 45 + (int) Math.round(coverage * 4));
		}
		return best;
	}

	/** Keyword alternatives for each fragment of the label, longest table entry first, left to right. */
	static List<List<String>> fragments(String compactLabel) {
		List<List<String>> result = new ArrayList<List<String>>();
		String rest = compactLabel;
		while (!rest.isEmpty()) {
			boolean matched = false;
			for (String[] entry : KEYWORDS) {
				int at = rest.indexOf(entry[0]);
				if (at != 0) continue;
				result.add(Arrays.asList(entry[1].split(" ")));
				rest = rest.substring(entry[0].length());
				matched = true;
				break;
			}
			if (!matched) {
				// Skip one character (particles, spaces already removed) and try again.
				int next = Integer.MAX_VALUE;
				for (String[] entry : KEYWORDS) { int at = rest.indexOf(entry[0]); if (at > 0) next = Math.min(next, at); }
				if (next == Integer.MAX_VALUE) break;
				rest = rest.substring(next);
			}
		}
		return result;
	}

	/** camelCase, snake_case, kebab-case and digits → lower-case tokens ("regDt" → [reg, dt]). */
	public static List<String> tokens(String propertyName) {
		List<String> tokens = new ArrayList<String>();
		String spaced = propertyName == null ? "" : propertyName.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2").replaceAll("[_\\-.$\\s]+", " ");
		for (String t : spaced.trim().toLowerCase(Locale.ROOT).split(" ")) { if (!t.isEmpty()) tokens.add(t); }
		return tokens;
	}

	static String compact(String text) { return text == null ? "" : text.replaceAll("[\\s*:：()\\[\\]/·ㆍ]+", ""); }

	private static OpenApiSpec.Property find(List<OpenApiSpec.Property> candidates, String name) {
		if (name == null) return null;
		for (OpenApiSpec.Property c : candidates) { if (c.name.equals(name.trim())) return c; }
		return null;
	}

	// ------------------------------------------------------------------ ai + cache

	private Map<String, String> ask(List<String> labels, List<OpenApiSpec.Property> candidates) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		JSONArray fields = new JSONArray();
		for (OpenApiSpec.Property c : candidates) fields.put(new JSONObject().put("name", c.name).put("description", c.description).put("type", c.type));
		String answer = client.completeJson(SYSTEM, "라벨: " + new JSONArray(labels) + "\n후보 필드: " + fields, null);
		if (answer == null) return result;
		try {
			int start = answer.indexOf('{');
			int end = answer.lastIndexOf('}');
			if (start < 0 || end <= start) return result;
			JSONObject map = new JSONObject(answer.substring(start, end + 1)).optJSONObject("map");
			if (map == null) return result;
			Set<String> used = new LinkedHashSet<String>();
			for (String label : labels) {
				String name = map.optString(label, "").trim();
				if (name.isEmpty() || used.contains(name) || find(candidates, name) == null) continue;
				used.add(name);
				result.put(label, name);
			}
		} catch (RuntimeException e) {
			ProgressLog.step("API 라벨 매칭 응답 파싱 실패: {}", e.getMessage());
		}
		return result;
	}

	private static String key(String scope, String label) { return (scope == null ? "" : scope.trim()) + "|" + compact(label).toLowerCase(Locale.ROOT); }

	private void load() {
		try {
			if (!cacheFile.isFile()) return;
			JSONObject json = new JSONObject(new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8));
			for (String key : json.keySet()) cache.put(key, json.getString(key));
		} catch (Exception e) {
			ProgressLog.step("API 라벨 캐시 읽기 실패: {}", e.getMessage());
		}
	}

	private void save() {
		try {
			File dir = cacheFile.getParentFile();
			if (!dir.isDirectory() && !dir.mkdirs()) return;
			JSONObject json = new JSONObject();
			for (Map.Entry<String, String> entry : cache.entrySet()) json.put(entry.getKey(), entry.getValue());
			Files.write(cacheFile.toPath(), json.toString(2).getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			ProgressLog.step("API 라벨 캐시 저장 실패: {}", e.getMessage());
		}
	}

	public int cacheSize() { return cache.size(); }
}
