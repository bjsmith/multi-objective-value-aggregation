package tools.spreadsheet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVWriter {

	public String filePath;

	public CSVWriter(String fileName) {
		this.filePath = fileName + ".csv";
		// create file
		File csvFile = new File(this.filePath);
		if(!csvFile.exists()) {
			try {
				csvFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String convertToCSV(String[] data) {
	    return Stream.of(data)
	      .collect(Collectors.joining(","));
	}
	
	public void writeLines(List<String[]> dataLines){
		FileWriter csvFileWriter;
		try {
			csvFileWriter = new FileWriter(this.filePath, true);
			try(PrintWriter pw = new PrintWriter(csvFileWriter)) {
				dataLines.stream()
				.map(this::convertToCSV)
				.forEach(pw::println);
				pw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLinesRaw(String[] data){
		FileWriter csvFileWriter;
		try {
			csvFileWriter = new FileWriter(this.filePath, true);

			List<String> dataLines = new ArrayList<>();
			for(int i = 0; i < data.length; i++) {
				dataLines.add(data[i]);
			}
			try(PrintWriter pw = new PrintWriter(csvFileWriter)) {
				dataLines.stream()
				.forEach(pw::println);
				pw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		CSVWriter csv = new CSVWriter("data/test");
		csv.writeLinesRaw(new String[] {"test", "test2,test3"});
	}

}
