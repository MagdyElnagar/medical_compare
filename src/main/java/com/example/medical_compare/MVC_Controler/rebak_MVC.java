package com.example.medical_compare.MVC_Controler;

import java.io.FileOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.medical_compare.MedicineRepository;
import com.example.medical_compare.MedicineService;
import com.example.medical_compare.model.product_rebak;
@Controller
public class rebak_MVC {
	

	@Autowired
	private MedicineRepository repository;
	@Autowired
	private MedicineService service;

	
	
	@PostMapping("/deleteAllRebak")
	public void deleteAllRebak() {

		try {
			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Products");

			// Header بس
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("Name");
			header.createCell(1).setCellValue("Store");

			FileOutputStream fos = new FileOutputStream("products.xlsx");
			workbook.write(fos);

			fos.close();
			workbook.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@GetMapping("/rebak")
	public String rebak(Model model) {

		List<product_rebak> products = service.readFromExcel();
		model.addAttribute("products", products);

		return "rebak";
	}

	@PostMapping("/delete_sginal_rebak")
	public String delete_sginal_rebak(@RequestParam String name, String store, Model model) {

		service.deleteSignalRowExcel(name, store);

		List<product_rebak> products = service.readFromExcel();
		model.addAttribute("products", products);

		return "rebak";
	}

	@PostMapping("/rebak_add")
	public String saveProduct(@RequestParam String prod_name, @RequestParam String store, Model model) {

		product_rebak p = new product_rebak();
		p.setProdName(prod_name);
		p.setStore(store);

		service.writeToExcel(p);

		// 🔥 اقرأ البيانات بعد الحفظ مباشرة
		List<product_rebak> products = service.readFromExcel();
		model.addAttribute("products", products);

		return "rebak"; // يروح يعرض الجدول
	}

}
