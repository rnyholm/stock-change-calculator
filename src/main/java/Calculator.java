import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Calculator {
    public static void main(String[] args) throws URISyntaxException, IOException, CsvException {
        URL urlIn = Calculator.class.getResource("/tickers.csv");
        FileReader fileReader = new FileReader(new File(urlIn.toURI()));
        FileWriter fileWriter = new FileWriter(new File("./ticker_changes.csv"));

        CSVReader csvReader = new CSVReaderBuilder(fileReader)
                .withSkipLines(1)
                .build();
        List<String[]> allData = csvReader.readAll();

        HashMap<String, BigDecimal> tickersAndChange = new HashMap<>();

        for (String[] row : allData) {
            String ticker = row[0];

            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            from.add(Calendar.MONTH, -6);

            Stock stock = YahooFinance.get(ticker, from, to, Interval.DAILY);

            BigDecimal closeThen = stock.getHistory().get(0).getClose();
            BigDecimal closeNow = stock.getHistory().get(stock.getHistory().size() - 1).getClose();

            BigDecimal change = closeNow.subtract(closeThen);

            BigDecimal changePercent = change.divide(closeThen, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
//            System.out.println(stock.getSymbol() + " changed: " + changePercent.toPlainString() + "%");

            tickersAndChange.put(ticker, changePercent);
        }

        tickersAndChange = sortByValue(tickersAndChange);

        tickersAndChange.entrySet().forEach(entry -> System.out.println(entry.getKey() + " change: " + entry.getValue() + "%"));

        CSVWriter writer = new CSVWriter(fileWriter, ',',
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        String[] header = { "Ticker", "Change" };
        writer.writeNext(header);

        tickersAndChange.forEach((key, value) -> {
            BigDecimal bd = value.setScale(2, RoundingMode.HALF_UP);
            String[] data = { key, bd.toPlainString() + "%" };
            writer.writeNext(data);
        });

        writer.close();
    }

    public static HashMap<String, BigDecimal> sortByValue(HashMap<String, BigDecimal> map) {
        HashMap<String, BigDecimal> temp = map.entrySet().stream()
                .sorted((b1, b2) -> b2.getValue().compareTo(b1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        return temp;
    }
}
