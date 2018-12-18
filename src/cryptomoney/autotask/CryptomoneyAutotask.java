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

import cryptomoney.autotask.rule.ActionType;
//import cbpdca.rule.Rule;
//import cbpdca.rule.RuleType;
//import cbpdca.rule.RuleAllowance;

import java.util.HashMap;
import java.util.ArrayList;

import lsoc.library.providers.logging.*;
import lsoc.library.utilities.Sleep;

import com.coinbase.exchange.api.*;
import com.coinbase.exchange.api.exchange.*;
import com.coinbase.exchange.api.accounts.*;
import com.coinbase.exchange.api.config.GdaxConfiguration;
import java.text.DecimalFormat;

//import org.springframework.web.client.RestTemplate;

import java.util.List;
import com.coinbase.exchange.api.exchange.GdaxExchangeImpl;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.deposits.DepositService;
import com.coinbase.exchange.api.withdrawals.WithdrawalsService;
import com.coinbase.exchange.api.payments.PaymentService;
import com.coinbase.exchange.api.exchange.GdaxExchangeImpl;
import com.coinbase.exchange.api.config.GdaxConfiguration;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class CryptomoneyAutotask
{

    public static Autotask app;
    public static GdaxExchangeImpl exchange;
    public static OrderService orderService;// = new OrderService(exchange);
    public static AccountService accountService;
    public static WithdrawalsService withdrawalsService;
    public static PaymentService paymentService;
    public static DepositService depositService;
    
    public static ILoggingProvider logProv = new LoggingProviderSimple();
    public static int iterationIntervalMS = 1000*5;
    public static DecimalFormat btcFormat = new DecimalFormat("#0.00000000");
    
    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args)
    { 
        CryptomoneyAutotask.logProv.LogMessage("version 0.06");
        
        if(args.length < 4)
        {
            String argMessage = "4 args are required:"
                    + "\n" +"apiPubKey"
                    + "\n" + "apiSecretKey"
                    + "\n" + "apiPassphrase"
                    + "\n" + "apiBaseURL";
            System.out.println(argMessage);
            CryptomoneyAutotask.logProv.LogMessage(argMessage);    
            
            System.exit(1);
        }
        
        Signature sig = new Signature(args[1]);
                
        exchange = new GdaxExchangeImpl(args[0], args[2], args[3], sig);
        
        orderService = new OrderService(exchange);
        accountService = new AccountService(exchange);
        withdrawalsService = new WithdrawalsService(exchange);
        paymentService = new PaymentService(exchange);
        depositService = new DepositService(exchange);
        
        app = new Autotask();
        app.Run();
        
    }
}
