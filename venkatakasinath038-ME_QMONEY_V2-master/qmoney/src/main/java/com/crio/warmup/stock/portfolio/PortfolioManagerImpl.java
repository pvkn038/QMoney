
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import java.util.Collections;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.sound.sampled.AudioInputStream;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {



  private String token = "6230d0b1d5d8070274502eef11cbeab14261a3a9";
  //private RestTemplate restTemplate = new RestTemplate();
  public  RestTemplate restTemplate = new RestTemplate();
  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.


  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate)
  {
    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> list = new ArrayList<AnnualizedReturn>();
    for (int i = 0; i < portfolioTrades.size(); i++) {
      annualizedReturn = getAnnualizedReturn(portfolioTrades.get(i), endDate);
      list.add(annualizedReturn);
    }
    Comparator<AnnualizedReturn> SortbyAnnReturn =
        Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
    Collections.sort(list, SortbyAnnReturn);
    return list;
  }

  public AnnualizedReturn getAnnualizedReturn(PortfolioTrade trade,LocalDate endLocalDate)
  {
    AnnualizedReturn annualizedReturn;
    String symbol = trade.getSymbol();
    LocalDate startLocalDate = trade.getPurchaseDate();

    try{
      List<Candle> StockStartToEndDate;
      StockStartToEndDate = getStockQuote(symbol, startLocalDate, endLocalDate);
      Candle stockStartDate = StockStartToEndDate.get(0);
      Candle stockLatest = StockStartToEndDate.get(StockStartToEndDate.size()-1);

      Double buyPrice = stockStartDate.getOpen();
      Double sellPrice = stockLatest.getClose();

      double totalReturn = (sellPrice - buyPrice) / buyPrice;
    double totalNumYears = ChronoUnit.DAYS.between(startLocalDate, endLocalDate) / 365.24;

    double annualizedReturns = Math.pow((1 + totalReturn), (1 / totalNumYears)) - 1;

    annualizedReturn = new AnnualizedReturn(symbol,annualizedReturns, totalReturn);
  } catch (JsonProcessingException e) {
    annualizedReturn = new AnnualizedReturn(symbol, Double.NaN, Double.NaN);
  }
  return annualizedReturn;
  }
  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    if(from.compareTo(to) >= 0)
    {
      throw new RuntimeException();
    }
    String url = buildUri(symbol, from, to);

    TiingoCandle[] stockStarttoEndDate = restTemplate.getForObject(url,TiingoCandle[].class);
    if (stockStarttoEndDate == null)
    {
      return new ArrayList<Candle>();
    }
    List<Candle> list = Arrays.asList(stockStarttoEndDate);
    return list;
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String TOKEN = token;
    String uriTemplate = 
        "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate="
            + startDate + "&endDate=" + endDate + "&token=" + TOKEN;
    return uriTemplate;
  }
}
