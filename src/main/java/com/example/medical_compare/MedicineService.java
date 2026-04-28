package com.example.medical_compare;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.medical_compare.model.product_rebak;

import ch.qos.logback.core.boolex.Matcher;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class MedicineService {

	// قائمة الكلمات التي يجب حذفها لأنها تغير اسم الصنف بدون داعي
	private static final List<String> STOP_WORDS = Arrays.asList("جديد", "سعر", "مخزن", "عرض", "قديم", "مميز", "تفتيح",
			"كريم", "اقراص", "قرص", "شراب", "ج", "س ج", "باكت", "مجم", "قـــــرص", "جديد", "الباكت", "باكو", "الباكو",
			"بالضمان", "كرتونه", "س  ج", "ك31", "ك81", "40", "48", "ك240", "ك120", "ك420", "ك160", "ك96", "ك80", "ك300",
			"ك590", "بالضمان");

	public String cleanMedicineName(String name) {
		if (name == null)
			return "";
		String clean;
		// إزالة التطويل والرموز
		name = name.replaceAll("٠", "0").replaceAll("١", "1").replaceAll("٢", "2").replaceAll("٣", "3")

				.replaceAll("٤", "4").replaceAll("٥", "5").replaceAll("٦", "6").replaceAll("٧", "7")
				.replaceAll("٨", "8").replaceAll("٩", "9").trim();

		// 2. normalization بسيط
		name = name.replaceAll("[أإآ]", "ا").replace("ة", "ه").replace("ى", "ي").replace("ـ", "").trim();

		// 3. dictionary للاستبدالات
		Map<String, String> replacements = new HashMap<>();
		replacements.put("كبسوله", "كبسول");
		replacements.put("كبسول", "كبسول");
		replacements.put("احترس", "");

		replacements.put("اقراص", "قرص");
		replacements.put("قرص", "قرص");
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

		// 4. إزالة الحاجات غير المهمة
		name = name.replaceAll("(?i)(سعر|جديد|قديم|س\\.ج|س ج)\\s*\\d*", " ")
				.replaceAll("(?i)(باكو|باكت|كرتونه)\\s*\\d+", " ").replaceAll("\\.xlsx|\\.xls", " ")
				.replaceAll("(?i)(احترس|ركز|قديم|جديد|جنية|جنيه)\\s*\\d+", " ")
				.replaceAll("[\\*\\-\\+\\=\\_\\#\\@\\!\\؟\\،\\؛]", " ").replaceAll("\\s+", " ").trim();
		name = name.replaceAll("/+$", "");
		return name;
	}

	public Medicine parseExcelRow(String rawName, Double price, Double discount, String warehouse) {
		Medicine med = new Medicine();
		med.setRawName(rawName);
		med.setPrice(price);
		med.setDiscount(discount);
		med.setWarehouse(warehouse);

		// 1. استخراج التركيز (الارقام المركبة)
		String regex = "\\d+(\\.\\d+)?(/\\d+(\\.\\d+)?)*";
		Pattern pattern = Pattern.compile(regex);
		java.util.regex.Matcher matcher = pattern.matcher(rawName);
		rawName = truncateAfterUnit(rawName);

		/*
		 * String strengthFound = ""; if (matcher.find()) { strengthFound =
		 * matcher.group(); med.setStrength(strengthFound); }
		 * 
		 * // 2. تنظيف الاسم التجاري (Brand Name) // أولاً: استبدال التركيز بمسافة (لحل
		 * المشكلة اللي ذكرتها حضرتك) String brandName = rawName;
		 * 
		 * // ثانياً: توحيد الحروف وحذف الرموز والكلمات المهملة brandName =
		 * brandName.toLowerCase().replaceAll("[أإآ]", "ا").replaceAll("ة",
		 * "ه").replaceAll("ى", "ي") .replaceAll("[/\\*\\-\\!\\(\\)]", " "); // حذف
		 * الرموز
		 * 
		 * if (!strengthFound.isEmpty()) { brandName = rawName.replace(strengthFound,
		 * " "); // وضعنا مسافة هنا }
		 * 
		 * // ثالثاً: حذف الكلمات المهملة (Stop Words) for (String word : STOP_WORDS) {
		 * brandName = brandName.replaceAll("\\b" + word + "\\b", " "); }
		 * 
		 * // رابعاً: تنظيف المسافات الزائدة brandName = brandName.replaceAll("\\s+",
		 * " ").trim();
		 */
		med.setBrandName(rawName);
		return med;
	}

	private String truncateAfterUnit(String name) {
		if (name == null)
			return "";

		// Regex يبحث عن رقم يليه مباشرة أو بمسافة وحدة قياس (عربي أو إنجليزي)
		// الوحدات: مجم، مل، جم، مج، mg, ml, gm, g
		String regex = "(مجم| مل | جم| مج |mg|ml|gm|g|mcg)";

		Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
		java.util.regex.Matcher matcher = pattern.matcher(name);

		if (matcher.find()) {
			// matcher.end() تعطينا موقع نهاية الوحدة (مثلاً بعد حرف الميم في مجم)
			// سنأخذ النص من البداية وحتى هذا الموقع فقط
			return name.substring(0, matcher.end()).trim();
		}

		return name;// إذا لم يجد وحدة، يعيد الاسم كما هو
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

		// threshold (تقدر تزبطه)
		return bestScore >= 0.88 ? bestMatch : null;
	}

	public List<Medicine> loadFromExcel(MultipartFile file) {

		List<Medicine> list = new ArrayList<>();

		try {
			Workbook workbook = WorkbookFactory.create(file.getInputStream());
			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {

				if (row.getRowNum() == 0)
					continue; // skip header

				String name = row.getCell(0).getStringCellValue();

				if (name.isEmpty())
					continue;

				Medicine med = new Medicine();
				med.setBrandName(name); // هنا بس الاسم

				list.add(med);
			}

			workbook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public void writeToExcel(product_rebak product) {
		String path = "products.xlsx";
		Workbook workbook = null;
		Sheet sheet = null;

		try {
			File file = new File(path);

			if (file.exists() && file.length() > 0) {
				// ✅ اقرأ الملف القديم
				FileInputStream fis = new FileInputStream(file);
				workbook = new XSSFWorkbook(fis);
				fis.close();

				sheet = workbook.getSheetAt(0);
			} else {
				// ✅ أنشئ ملف جديد
				workbook = new XSSFWorkbook();
				sheet = workbook.createSheet("Products");

				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("Name");
				header.createCell(1).setCellValue("Store");
			}

			// ✅ حدد آخر صف صح
			int lastRow = sheet.getLastRowNum() + 1;
			Row row = sheet.createRow(lastRow);

			row.createCell(0).setCellValue(product.getProdName());
			row.createCell(1).setCellValue(product.getStore());

			// ✅ اكتب في الملف
			FileOutputStream fos = new FileOutputStream(path);
			workbook.write(fos);

			fos.close();
			workbook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	

	public List<product_rebak> readFromExcel() {
		List<product_rebak> list = new ArrayList<>();

		try {
			File file = new File("products.xlsx");

			if (!file.exists() || file.length() == 0) {
				return list; // 🔥 مهم
			}

			FileInputStream fis = new FileInputStream(file);
			Workbook workbook = new XSSFWorkbook(fis);
			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;

				product_rebak p = new product_rebak();
				p.setProdName(row.getCell(0).getStringCellValue());
				p.setStore(row.getCell(1).getStringCellValue());

				list.add(p);
			}

			workbook.close();
			fis.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public void deleteSignalRowExcel(String name, String store) {

		try {
			File file = new File("products.xlsx");

			if (!file.exists() || file.length() == 0) {
				return;
			}

			FileInputStream fis = new FileInputStream(file);
			Workbook workbook = new XSSFWorkbook(fis);
			Sheet sheet = workbook.getSheetAt(0);

			int rowIndexToDelete = -1;

			for (Row row : sheet) {
				if (row.getRowNum() == 0)
					continue;

				String prodName = row.getCell(0).getStringCellValue();
				String storeName = row.getCell(1).getStringCellValue();

				if (prodName.equals(name) && storeName.equals(store)) {
					rowIndexToDelete = row.getRowNum();
					break;
				}
			}

			if (rowIndexToDelete != -1) {
				int lastRowNum = sheet.getLastRowNum();

				if (rowIndexToDelete >= 0 && rowIndexToDelete < lastRowNum) {
					sheet.shiftRows(rowIndexToDelete + 1, lastRowNum, -1);
				} else {
					Row removingRow = sheet.getRow(rowIndexToDelete);
					if (removingRow != null) {
						sheet.removeRow(removingRow);
					}
				}
			}

			fis.close();

			FileOutputStream fos = new FileOutputStream(file);
			workbook.write(fos);
			fos.close();
			workbook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

}
