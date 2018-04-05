package ai.api.examples;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;

public class Seperate {

	public void apiai(String text) throws ParseException {
		// String text = "나는 충남대학교에 다닌다"; // 분석할 텍스트 데이터

		String[] sSPO = new String[3];

		try {

			InputStreamReader streamReader = new InputStreamReader(System.in);
			BufferedReader bufferedReader = new BufferedReader(streamReader);

			if (text.contains(":newstore")) { // 새로운 상점
				String newStore = text.replace(text.trim().split(" ")[0], "").trim();
				System.out.println(newStore);
				teachNewStore(newStore);
			}

			else if (text.contains(":teach")) { // 가르치기
				System.out.print("가르칠 음식점을 입력해주세요 : ");
				String textStore = text.replace(text.trim().split(" ")[0], "").trim();
				sSPO[0] = textStore.trim();
				System.out.print("1.주소, 2.영업 시간, 3.메뉴, 4.사이트 > ");
				String Predicate = bufferedReader.readLine();
				// sSPO[1] = Integer.parseInt(Predicate);
				sSPO[1] = Predicate.trim();
				System.out.print(Predicate + "을(를) 입력해주세요");
				String Objective = bufferedReader.readLine();
				sSPO[2] = Objective.trim();
				// System.out.println("SPO->" + sSPO[0] + "/" + sSPO[1] + "/" + sSPO[2]);
				teachStoreInfo(sSPO[1], sSPO[2]);

			} else { // 질문하기
				Question(text.trim());
				System.out.println("++++++++++++++++");
				QuestionEval(text);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 가르치고자 한ㄴ 음식점 : 별리달리 "별리달리는 : "
	 */

	public static int position(String input) {
		int position = -1;
		switch (input) {
		case "ARG0":
			position = 0;
			break;
		case "ARG1":
		case "ARG2":
		case "ARG3":
		case "ARG4":
			position = 2;
			break;
		default:
			position = -1;

		}
		return position;
	}

	public static void Question(String text) throws ParseException, org.json.simple.parser.ParseException {
		String openApiURL = "http://aiopen.etri.re.kr:8000/WiseQAnal";
		String accessKey = "d303f91a-0f8f-4f58-acab-5d85944807ff"; // 발급받은 Access Key
		String analysisCode = "SRL"; // 언어 분석 코드
		Gson gson = new Gson();

		Map<String, Object> request = new HashMap<>();
		Map<String, String> argument = new HashMap<>();

		argument.put("analysis_code", analysisCode);
		argument.put("text", text);

		request.put("access_key", accessKey);
		request.put("argument", argument);

		URL url;
		Integer responseCode = null;
		String responBody = null;
		try {
			url = new URL(openApiURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("content-type", "application/json; charset=utf-8");
			con.setRequestMethod("POST");
			con.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.write(gson.toJson(request).getBytes("UTF-8"));
			wr.flush();
			wr.close();

			responseCode = con.getResponseCode();
			InputStream is = con.getInputStream();
			byte[] buffer = new byte[is.available()];
			int byteRead = is.read(buffer);
			responBody = new String(buffer);
			JSONParser parser = new JSONParser();
			JSONObject root = (JSONObject) parser.parse(responBody);
			JSONObject return_object = (JSONObject) root.get("return_object");
			JSONObject orgQInfo = (JSONObject) return_object.get("orgQInfo");
			JSONObject orgQUnit = (JSONObject) orgQInfo.get("orgQUnit");
			JSONObject nDoc = (JSONObject) orgQUnit.get("ndoc");

			JSONArray sentence = (JSONArray) nDoc.get("sentence");
			JSONObject morp_bf = (JSONObject) sentence.get(0);
			JSONArray MORPArray = (JSONArray) morp_bf.get("morp");

			ArrayList<String> NNGList = new ArrayList<String>();
			for (int MCount = 0; MCount < MORPArray.size(); MCount++) {
				JSONObject Mtemp = (JSONObject) MORPArray.get(MCount);
				String type = (String) Mtemp.get("type");
				String NNGString = (String) Mtemp.get("lemma");

				if (type.contains("NN")) {
					NNGList.add(NNGString);
				}
			}

			for (int nngcount = 0; nngcount < NNGList.size(); nngcount++) {
				System.out.println(NNGList.get(nngcount));
			}
			System.out.println(text.split(NNGList.get(1))[0]);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void QuestionEval(String text) throws ParseException, org.json.simple.parser.ParseException {
		String openApiURL = "http://aiopen.etri.re.kr:8000/WiseQAnal";
		String accessKey = "d303f91a-0f8f-4f58-acab-5d85944807ff"; // 발급받은 Access Key
		String analysisCode = "SRL"; // 언어 분석 코드
		Gson gson = new Gson();

		Map<String, Object> request = new HashMap<>();
		Map<String, String> argument = new HashMap<>();

		argument.put("analysis_code", analysisCode);
		argument.put("text", text);

		request.put("access_key", accessKey);
		request.put("argument", argument);

		URL url2;
		Integer responseCode2 = null;
		String responBody2 = null;
		String predicate = "";

		try {
			url2 = new URL(openApiURL);
			HttpURLConnection con2 = (HttpURLConnection) url2.openConnection();
			con2.setRequestProperty("content-type", "application/json; charset=utf-8");
			con2.setRequestMethod("POST");
			con2.setDoOutput(true);

			DataOutputStream wr2 = new DataOutputStream(con2.getOutputStream());
			wr2.write(gson.toJson(request).getBytes("UTF-8"));
			wr2.flush();
			wr2.close();

			responseCode2 = con2.getResponseCode();
			InputStream is = con2.getInputStream();
			byte[] buffer = new byte[is.available()];
			int byteRead = is.read(buffer);
			responBody2 = new String(buffer);
			JSONParser parser = new JSONParser();
			JSONObject root = (JSONObject) parser.parse(responBody2);
			JSONObject return_object = (JSONObject) root.get("return_object");
			JSONObject orgQInfo = (JSONObject) return_object.get("orgQInfo");
			JSONObject orgQUnit = (JSONObject) orgQInfo.get("orgQUnit");
			JSONObject nDoc = (JSONObject) orgQUnit.get("ndoc");
			JSONArray sentence = (JSONArray) nDoc.get("sentence");
			JSONObject morp_bf = (JSONObject) sentence.get(0);
			JSONArray MORPArray = (JSONArray) morp_bf.get("morp_eval");
			JSONArray vLATs = (JSONArray) orgQUnit.get("vLATs");

			if (vLATs.size() != 0) { // 어휘정답유형 존재할 경우 - Predicate로 취급해준다
				JSONObject strLAT_o = (JSONObject) vLATs.get(0);
				String strLAT = (String) strLAT_o.get("strLAT");
				System.out.println("ASSUMED PREDICATE = " + strLAT);
				predicate = strLAT;
			}

			ArrayList<String> NNGList = new ArrayList<String>(); // 명사 리스트

			int real_NNG = 0;

			for (int MCount = 0; MCount < MORPArray.size(); MCount++) {
				JSONObject Mtemp = (JSONObject) MORPArray.get(MCount);
				String type = (String) Mtemp.get("result");
				String NNGString = (String) Mtemp.get("target");
				if (type.contains("/NNG") && !type.contains("/V")) { // 진짜 명사인 애들
					real_NNG++;
					NNGList.add(NNGString);
				} else {
					if (real_NNG <= 1) {
						NNGList.add(NNGString);
					}
				}

			}

			predicate = returnStoreCandidate(NNGList);
			for (int nngcount = 0; nngcount < NNGList.size(); nngcount++) {
				System.out.println(NNGList.get(nngcount));
			}
			// System.out.println("추측 가게 + " + AssumeStore);
			System.out.println("추측 가게 (DB 뒤짐) + " + SearchDB_obj_StoreName(NNGList.get(0)));

			///////////////// 테스트중
			DecideWhichStore(NNGList);

			System.out.println("Predicate -> " + predicate);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String returnStoreCandidate(ArrayList<String> arr) {
		String deleteTarget_predicate = arr.get(arr.size() - 1);
		arr.remove(arr.get(arr.size() - 1));
		return deleteTarget_predicate;

	}

	public static void searchByWord(ArrayList<String> arr) {
		String findTarget = "";
		ArrayList<String> resultTempArr = new ArrayList<String>();
		for (int accum_count = 0; accum_count < arr.size() - 1; accum_count++) {
			findTarget += arr.get(accum_count);
			System.out.println(SearchDB_obj_StoreName(findTarget).get(0));
		}
	}

	public void teachNewStore(String newStore) throws IOException {

		// final String UPDATE_TEMPLATE = "PREFIX store: <http://localhost:3030/store#>"
		// + "INSERT DATA"
		// + "{ <http://localhost:3030/store> store:name \"" + newStore.replace(" ", "")
		// + "\" ." + "} ";

		final String UPDATE_TEMPLATE = "PREFIX store: <http://localhost:3030/store#> "
				+ " PREFIX rdf: <http://localhost:3030/store#>" + "INSERT DATA"
				+ "{ <http://localhost:3030/store#%s>    store:name    \"" + newStore.replace(" ", "") + "\" ."
				+ "}   ";

		String id = UUID.randomUUID().toString();

		// createDataset();
		// System.out.println(String.format("Adding %s", id));
		UpdateProcessor upp = UpdateExecutionFactory.createRemote(
				UpdateFactory.create(String.format(UPDATE_TEMPLATE, id)), "http://localhost:3030/store/update");
		upp.execute();
		// String URL = "http://localhost:3030/";
		// String subURL = URLEncoder.encode(newStore, "UTF-8");
		// System.out.println(URL + subURL);
		// TDBFactory.createDataset("http://localhost:3030/");
	}

	public void teachStoreInfo(String predicate, String Objective) {
		final String UPDATE_TEMPLATE = "PREFIX store: <http://localhost:3030/store#>" + "INSERT DATA"
				+ "{ <http://localhost:3030/store>     store:site   \"" + Objective + "\" ." + "}   ";

		String id = UUID.randomUUID().toString();
		// System.out.println(String.format("Adding %s", id));
		UpdateProcessor upp = UpdateExecutionFactory.createRemote(
				UpdateFactory.create(String.format(UPDATE_TEMPLATE, id)), "http://localhost:3030/store/update");
		upp.execute();
		// String URL = "http://localhost:3030/";
	}

	public static Dataset createDataset() throws IOException {
		URL url = new URL("http://localhost:3030/");
		URLConnection con = url.openConnection();
		HttpURLConnection http = (HttpURLConnection) con;
		http.setRequestMethod("POST"); // PUT is another valid option
		http.setDoOutput(true);
		Dataset dataset = TDBFactory.createDataset("http://localhost:3030/testServer");
		return dataset;
	}

	public void serachPredicate(String p) {

	}

	public static ArrayList<String> SearchDB_obj_StoreName(String input) { // input
		final String UPDATE_TEMPLATE = "SELECT ?object " + "WHERE { " + "<http://localhost:3030/store> "
				+ " <http://localhost:3030/store#name> " + " ?object filter contains(?object,\"" + input + "\") . "
				+ "} ";
		String queryService = "http://localhost:3030/store/sparql";
		QueryExecution q = QueryExecutionFactory.sparqlService(queryService, UPDATE_TEMPLATE);
		ResultSet results = q.execSelect();
		ArrayList<String> resultArr = new ArrayList<String>(); // 지식베이스에서 일치하는 거 리턴한 List
		while (results.hasNext()) {
			QuerySolution soln = results.nextSolution();
			// assumes that you have an "?x" in your query
			RDFNode x = soln.get("object");
			if (!resultArr.contains(x.toString()))
				resultArr.add(x.toString()); // object 후보 애들 다 넣어주기. 일단 contains query를 씀 - like 사용 시 수정
		}
		return resultArr;
	}

	public static String SearchDB_obj_StoreName_Exact(String input) { // 완벽히 일치하는 상점 이름 찾기
		String ExactStore = null;
		final String UPDATE_TEMPLATE = "SELECT ?object " + "WHERE { " + "<http://localhost:3030/store> "
				+ " <http://localhost:3030/store#name> " + " \"" + input + "\" . " + "} ";
		String queryService = "http://localhost:3030/store/sparql";
		QueryExecution q = QueryExecutionFactory.sparqlService(queryService, UPDATE_TEMPLATE);
		ResultSet results = q.execSelect();
		if (results.getRowNumber() == 1) {
			QuerySolution soln = results.nextSolution();
			// assumes that you have an "?x" in your query
			RDFNode x = soln.get("object");
			ExactStore = x.toString().substring(1, x.toString().length() - 2);
		}
		return ExactStore;
	}

	/**
	 * 검색 알고리즘은 특정 단어를 포함하는 단어중 공통점이 많은 부분만 ... 가게명이 ABCD일경우 A검색 List B검색 List C검색
	 * List AB 검색 List BC 검색 List ABC 검색 List 검색 후 결과가 있다면 그걸 Store로 결정
	 */

	@SuppressWarnings({ "unchecked", "rawtypes", "unlikely-arg-type" }) //// predicate 제거하기 추가해야됨!!!
	public static void DecideWhichStore(ArrayList<String> candidates) {
		ArrayList<String> searchResults = new ArrayList<String>();
		String simple = "";
		for (String s : candidates) {
			simple += s;
			searchResults.addAll(DecideStoreBySplit(simple));
		}
		String avgString = "";
		HashMap<String, Integer> freqCount = new HashMap<String, Integer>();
		for (String word : searchResults) {
			Integer f = freqCount.get(word);
			freqCount.put(word, f + 1);
		}

		TreeMap<String, Integer> tm = new TreeMap<String, Integer>(freqCount);
		Iterator<Integer> iteratorKey = tm.values().iterator(); // 키값 오름차순 정렬(기본)

		while (iteratorKey.hasNext()) {
			Integer key = iteratorKey.next();
			System.out.println(key + "," + tm.get(key));
		}
		/**
		 * Stream.of(searchResults) .collect(Collectors.groupingBy(s -> s,
		 * Collectors.counting())) .entrySet() .stream()
		 * .max(Comparator.comparing(Entry::getValue)) .ifPresent(System.out::println);
		 */
		System.out.println("Testing....." + simple);// 통째로 검색하기
		if (SearchDB_obj_StoreName_Exact(simple) != null) { // 정확히 일치하는

		}
	}

	/** A,B,C,D 등 작게 쪼갠 string을 검색해서 포함 string 을 리스트로 반환 */
	public static ArrayList<String> DecideStoreBySplit(String input) {
		final String UPDATE_TEMPLATE = "SELECT ?object " + "WHERE { " + "<http://localhost:3030/store> "
				+ " <http://localhost:3030/store#name> " + " ?object filter contains(?object,\"" + input + "\") . "
				+ "} ";
		String queryService = "http://localhost:3030/store/sparql";
		QueryExecution q = QueryExecutionFactory.sparqlService(queryService, UPDATE_TEMPLATE);
		ResultSet results = q.execSelect();
		ArrayList<String> resultArr = new ArrayList<String>(); // 지식베이스에서 일치하는 거 리턴한 List
		while (results.hasNext()) {
			QuerySolution soln = results.nextSolution();
			// assumes that you have an "?x" in your query
			RDFNode x = soln.get("object");
			if (!resultArr.contains(x.toString()))
				resultArr.add(x.toString()); // object 후보 애들 다 넣어주기. 일단 contains query를 씀 - like 사용 시 수정
		}
		return resultArr;
	}
}
