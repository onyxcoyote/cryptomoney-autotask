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
package cryptomoney.autotask;

import java.util.ArrayList;
import java.text.DecimalFormat;
import java.util.List;

import com.coinbase.exchange.api.exchange.*;
import com.coinbase.exchange.api.accounts.*;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.deposits.DepositService;
import com.coinbase.exchange.api.withdrawals.WithdrawalsService;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.payments.PaymentService;
import com.coinbase.exchange.api.exchange.GdaxExchangeImpl;

import lsoc.library.settings.FileConfig;
import lsoc.library.providers.logging.*;
import lsoc.library.providers.logging.ILoggingProvider;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class CryptomoneyAutotask
{

    public static Autotask app;
    public static GdaxExchangeImpl exchange;
    public static OrderService orderService;
    public static AccountService accountService;
    public static WithdrawalsService withdrawalsService;
    public static PaymentService paymentService;
    public static DepositService depositService;
    public static MarketDataService marketDataService;
    
    public static ILoggingProvider logProv = new LoggingProviderSimple();
    private static ILoggingProvider logProvFile;
    public static ILoggingProvider logMultiplexer;
    public static int iterationIntervalMS = 1000*5;
    public static DecimalFormat coinFormat = new DecimalFormat("#0.00000000");
    public static DecimalFormat fiatFormat = new DecimalFormat("#0.00");
    
    public static FileConfig config;
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    { 
        try
        {
            String version = "0.16";

            try
            {
                logProvFile = new LoggingProviderFlatFile("CryptomoneyAutotask");
                List<ILoggingProvider> loggingProviders = new ArrayList<>();
                loggingProviders.add(logProv);
                loggingProviders.add(logProvFile);
                
                logMultiplexer = new LoggingProviderMultiplexer(loggingProviders);
            }
            catch(Exception ex)
            {
                logProv.LogException(ex);
                System.exit(1);
            }
            CryptomoneyAutotask.logMultiplexer.LogMessage("version "+version);
            CryptomoneyAutotask.logMultiplexer.LogMessage("program starting");

            //Load config settings
            config = new FileConfig(logMultiplexer);
            String apiPubKey = config.getConfigString("api_pub_key");
            String apiSecretKey = config.getConfigString("api_secret_key");
            String apiPassphrase = config.getConfigString("api_passphrase");
            String apiBaseURL = config.getConfigString("api_base_url");
            String executeImmediately = config.getConfigString("execute_immediately");
            boolean bExecuteImmediately = Boolean.parseBoolean(executeImmediately); //allow "true"
            logProv.LogMessage("config setting executeImmediately: " + bExecuteImmediately);

            
            //initialize gdax-java objects, using it as a library
            Signature sig = new Signature(apiSecretKey);
            exchange = new GdaxExchangeImpl(apiPubKey, apiPassphrase, apiBaseURL, sig);
            orderService = new OrderService(exchange);
            accountService = new AccountService(exchange);
            withdrawalsService = new WithdrawalsService(exchange);
            paymentService = new PaymentService(exchange);
            depositService = new DepositService(exchange);
            marketDataService = new MarketDataService(exchange);
            
            app = new Autotask(bExecuteImmediately);
            app.Run();
        }
        catch(Exception ex)
        {
            CryptomoneyAutotask.logMultiplexer.LogException(ex);
            System.exit(1);
        }
    }
}
