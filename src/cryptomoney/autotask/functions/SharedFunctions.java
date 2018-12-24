/*
    This file is part of cryptomoney-autotask.

    cryptomoney-autotask is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    cryptomoney-autotask is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with cryptomoney-autotask.  If not, see <https://www.gnu.org/licenses/>.
 */
package cryptomoney.autotask.functions;

import com.coinbase.exchange.api.marketdata.MarketData;
import com.coinbase.exchange.api.marketdata.OrderItem;
import cryptomoney.autotask.CryptomoneyAutotask;
import java.math.BigDecimal;
import java.util.Random;

import lsoc.library.providers.logging.ILoggingProvider;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class SharedFunctions
{
    public static double GetNumberOfSystemIntervalsPerDay()
    {
        int msPerDay = 1000*60*60*24;
        double intervalsPerDay =  msPerDay / CryptomoneyAutotask.iterationIntervalMS;
        return intervalsPerDay;
    }
    
    public static double GenerateRandomNumberFrom1to100()
    {
        Random random = new Random();
        
        int num = random.nextInt(100)+1;
        return num;
    }
    
    public static boolean RollDie(double percentChangeBetween0and1, ILoggingProvider logProv)
    {
        Random random = new Random();
        
        int num = random.nextInt(100)+1;
    
        double numPct = num/100.0;
        
        
        if(numPct <= percentChangeBetween0and1)
        {
            logProv.LogMessage("rolldie: " + numPct + "/" + percentChangeBetween0and1 + "=true");
            return true;
        }
        else
        {
            logProv.LogMessage("rolldie: " + numPct + "/" + percentChangeBetween0and1 + "=false");
            return false;
        }
        
    }
    
    public static BigDecimal GetBestBTCSellPrice()
    {
        BigDecimal btcPrice = null;
        int level = 1; //level 1 is only the best bid and ask
        MarketData marketData = CryptomoneyAutotask.marketDataService.getMarketDataOrderBook("BTC-USD", Integer.toString(level));
        for(OrderItem orderItem : marketData.getAsks()) //ask = sell offer
        {
            //level 1 should only return 1 item
            btcPrice = orderItem.getPrice();
        }
        
        if(btcPrice == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("btc price cannot be null");
            System.exit(1); //don't know what else to do
        }
        
        if(btcPrice.doubleValue() < CryptomoneyAutotask.BTC_PRICE_MIN_REALISTIC)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("btc price cannot exceed min realistic");
            System.exit(1); //this means a hard-coded change is needed
        }
        
        if(btcPrice.doubleValue() > CryptomoneyAutotask.BTC_PRICE_MAX_REALISTIC)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("btc price cannot exceed max realistic");
            System.exit(1); //this means a hard-coded change is needed
        }
        
        return btcPrice;
    }
    
    public static BigDecimal GetBestBTCBuyPrice()
    {
        BigDecimal btcPrice = null;
        int level = 1; //level 1 is only the best bid and ask
        MarketData marketData = CryptomoneyAutotask.marketDataService.getMarketDataOrderBook("BTC-USD", Integer.toString(level));
        for(OrderItem orderItem : marketData.getBids()) //bid = buy offer
        {
            //level 1 should only return 1 item
            btcPrice = orderItem.getPrice();
        }
        
        if(btcPrice == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("btc price cannot be null");
            System.exit(1); //don't know what else to do
        }
        
        if(btcPrice.doubleValue() < CryptomoneyAutotask.BTC_PRICE_MIN_REALISTIC)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("btc price cannot exceed min realistic");
            System.exit(1); //this means a hard-coded change is needed
        }
        
        if(btcPrice.doubleValue() > CryptomoneyAutotask.BTC_PRICE_MAX_REALISTIC)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("btc price cannot exceed max realistic");
            System.exit(1); //this means a hard-coded change is needed
        }
        
        return btcPrice;
    }
}
