package com.example.medical_compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MedicineService {

	private static final List<String> STOP_WORDS = Arrays.asList("جديد", "سعر", "مخزن", "عرض", "قديم", "مميز", "تفتيح",
			"كريم", "اقراص", "قرص", "شراب", "ج", "س ج", "باكت", "مجم", "قـــــرص", "جديد", "الباكت", "باكو", "الباكو",
			"بالضمان", "كرتونه", "س  ج", "ك31", "ك81", "40", "48", "ك240", "ك120", "ك420", "ك160", "ك96", "ك80", "ك300",
			"ك590", "بالضمان");

	public String cleanMedicineName(String name) {
		if (name == null)
			return "";

		name = name.replaceAll("٠", "0").replaceAll("١", "1").replaceAll("٢", "2").replaceAll("٣", "3")
				.replaceAll("٤", "4").replaceAll("٥", "5").replaceAll("٦", "6").replaceAll("٧", "7")
				.replaceAll("٨", "8").replaceAll("٩", "9").trim();

		name = name.replaceAll("[أإآ]", "ا").replace("ة", "ه").replace("ى", "ي").replace("ـ", "").trim();

		Map<String, String> replacements = new HashMap<>();
		replacements.put("كبسوله", "كبسول");
		replacements.put("كبسولة", "كبسول");
		

		replacements.put("احترس", "");
		replacements.put("شريط", "");
		replacements.put("الفرعونيه", "");
		replacements.put("فاركو", "");
		replacements.put("المصريه", "");
		replacements.put("يونيفارما", "");
		replacements.put("لينكو فارم", "");
		replacements.put("انترفارم", "");
		replacements.put("الاوروبيه", "");
		replacements.put("العامريه", "");
		replacements.put("ايفا", "");
		replacements.put("ايفا فارم", "");
		replacements.put("مينافارم", "");
		replacements.put("مينا فارم", "");
		replacements.put("استلام ", "");
		replacements.put("شركات ", "");
		replacements.put("فقط", "");
		replacements.put("وول فارم", "");
		replacements.put("فايزر", "");
		replacements.put("بارك فيل", "");
		replacements.put("المهن", "");
		replacements.put("اكديما", "");
		replacements.put("ممنوعه", "");
		replacements.put("صغيره", "");
		replacements.put("علبه", "");
		replacements.put("كبيره", "");
		replacements.put("دراج فارم", "");
		replacements.put("تشغيله", "");
		replacements.put("القاهره", "");
		 


		replacements.put("اقراص", "قرص");
		replacements.put(" بلس", " plus ");
		replacements.put(" بلاس", " plus ");
		replacements.put(" بلاص", " plus ");
		replacements.put("نقط", "قطره");
		replacements.put("ممتد المفعول", "اكس ار");
		replacements.put("فوار", "اكياس");
		replacements.put("حبيبات", "اكياس");

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			name = name.replace(entry.getKey(), entry.getValue());
		}

		name = name.replaceAll("(?i)(سعر|جديد|قديم|جنيه|س\\.ج|س ج)\\s*\\d*", " ")
				.replaceAll("(?i)(باكو|باكت|كرتونه)\\s*\\d+", " ").replaceAll("\\.xlsx|\\.xls", " ")
				.replaceAll("(?i)(احترس|ركز|قديم|جديد|جنية|جنيه)\\s*\\d+", " ")
				.replaceAll("[\\*\\-\\+\\=\\_\\#\\@\\!\\؟\\،\\؛]", " ").replaceAll("\\s+", " ").trim();
		name = name.replaceAll("\\s*/(\\d{4,}|\\S+)?\\s*$", "").trim();	
		name = name.replaceAll("\\s+", " ").trim();
		name = name.replaceAll("كبسول", "قرص").trim();	
		name = name.replaceAll("\\s+", " ").trim();
		return name;
	}

	public Medicine parseExcelRow(String rawName, Double price, Double discount, String warehouse) {
		Medicine med = new Medicine();
		med.setRawName(rawName);
		med.setPrice(price);
		med.setDiscount(discount);
		med.setWarehouse(warehouse);

		rawName = truncateAfterUnit(rawName);
		med.setBrandName(rawName);
		return med;
	}

	private String truncateAfterUnit(String name) {
		if (name == null)
			return "";

		String regex = "(مجم| مل | جم| مج |mg|ml|gm|g|mcg)";
		Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
		java.util.regex.Matcher matcher = pattern.matcher(name);

		if (matcher.find()) {
			return name.substring(0, matcher.end()).trim();
		}
		return name;
	}

	public double similarity(String s1, String s2) {
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
		return jw.apply(s1, s2);
	}

	public Medicine findBestMatch(String name, List<Medicine> allMedicines) {
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
		double bestScore = 0;
		Medicine bestMatch = null;

		for (Medicine med : allMedicines) {
			if (med.getBrandName() == null)
				continue;
			double score = jw.apply(cleanMedicineName(med.getBrandName()), cleanMedicineName(name));
			if (score > bestScore) {
				bestScore = score;
				bestMatch = med;
			}
		}
		return bestScore >= 0.88 ? bestMatch : null;
	}

	public List<Medicine> loadFromExcel(MultipartFile file) {
		List<Medicine> list = new ArrayList<>();
		try {
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);
			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;
				String name = row.getCell(0).getStringCellValue();
				if (name.isEmpty())
					continue;
				Medicine med = new Medicine();
				med.setBrandName(name);
				list.add(med);
			}
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}
