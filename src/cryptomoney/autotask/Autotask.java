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

import cryptomoney.autotask.exchangeaccount.ExchangeAccount;
import cryptomoney.autotask.rule.ActionType;
import cryptomoney.autotask.rule.Rule;
import cryptomoney.autotask.rule.RuleAction;
import cryptomoney.autotask.rule.RuleAllowance;
import cryptomoney.autotask.rule.RuleType;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.exchange.GdaxExchangeImpl;
import com.coinbase.exchange.api.exchange.Signature;
import java.text.DecimalFormat;
import java.util.ArrayList;
import lsoc.library.providers.logging.ILoggingProvider;
import lsoc.library.providers.logging.LoggingProviderSimple;

/*import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;*/

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
/*@Component
@Configuration
@ComponentScan(basePackages = {"com.coinbase.exchange.api.exchange.Signature","com.coinbase.exchange.api.orders.OrderService","com.coinbase.exchange.api.exchange.GdaxExchangeImpl"})
@ComponentScan({"com.coinbase.exchange.api.exchange.Signature","com.coinbase.exchange.api.orders.OrderService","com.coinbase.exchange.api.exchange.GdaxExchangeImpl"})
//@Controller
*/
public class Autotask
{
    
    public ExchangeAccount account1;


    
    
    public boolean running = false;
    //public static HashMap<Integer, Account> accounts = new HashMap<>();
    
    
    public ArrayList<Rule> rules = new ArrayList<>();
    
    //public static int iterationIntervalMS = 1000*60*5; //5 minutes
    

    
    //public ILoggingProvider logProv = new LoggingProviderSimple();
  
    /*public void run(String[] args) throws Exception 
    {
        
    }*/
    
    
    //@Autowired
    public Autotask()
    {
        account1 = new ExchangeAccount();
        
    }
    
    /**
     * @return 
     */
    public OrderService orderService()
    { 
        return CryptomoneyAutotask.orderService;
        //return new OrderService(); 
        //return orderService;
    }
    
    public void Run()
    {

                
        Initialize();
        
        CancelOpenOrders();
        
        DoMainLoop();
    }
    
    private void Initialize()
    {
        LoadAccounts();
        LoadRules();

                
        //GdaxApiApplication app = new GdaxApiApplication();
        //GdaxConfiguration config = new GdaxConfiguration();
        
    }
    
    private void CancelOpenOrders()
    {
        account1.ProcessBTCBuyOrders(true);
    }
    
        
    private void LoadAccounts()
    {
        /*
            for each accuont load API key, description, etc.
        
        */
        
        //todo: un hard code
        
        /*      
        String publicKey = "";
        String passphrase = "";
        String baseUrl = "";
        String secretKey = "";
        Signature sig = new Signature(secretKey);
        RestTemplate restTemplate = new RestTemplate();
        
        gdax = new GdaxExchangeImpl(
                            publicKey, 
                            passphrase, 
                            baseUrl,
                            sig,
                            restTemplate);
        */
        
        
        
        //accounts.put(1, account1); //account #1
        
        
    }
    
    private void LoadRules()
    {
        //todo: un-hard code rules
        
        //ALLOWANCE RULES
        //account 1 - ALLOWANCE  buy_bitcoin $21/day (divided per hour)
        double ALLOWANCE_USD_AMOUNT_PER_DAY = 16000.00; //TODO: test
        RuleAllowance allowance1 = new RuleAllowance(RuleType.ALLOWANCE, ActionType.ALLOWANCE_BUY_BTC_POSTONLY, ALLOWANCE_USD_AMOUNT_PER_DAY); 
        rules.add(allowance1);
        /*
            TO ADD
            -account 1 - ALLOWANCE  coinbase_pro_to_coinbase $20/day (divided per hour)
            -account 1 - ALLOWANCE  deposit USD $22/day (divided per hour)
        */
        
        
        //ALARM RULES
        /*
            TO ADD
            -account 1 - ALARM if buy_btc_allowance > $20 and USD balance < $21
            -account 1 - ALARM if transfer_btc_coinbase_pro_to_coinbase_allowance > $20 and BTC balance < $21->bitcoin
            -account 1 - ALARM if coinbase_btc_balance <  $50->BTC, max once per day?
        */
        
        
        
        //ACTION RULES
        //account 1 - ACTION buy (w/ BUYMETHOD) around X bitcoin per day using USD, threshold 0.001 bitcoin, max .005 bitcoin, Do 25% of the time (randomness)
        double BUY_USD_AMOUNT_PER_DAY = 21.00;
        double COINBASE_PRO_MINIMUM_BTC_TRADE_THRESHOLD = 0.001;
        double PERCENT_OF_TIME_TO_DO_ACTION_WHEN_TRIGGERED = 0.25; //allows some randomness so it doesn't always happen
        RuleAction action1 = new RuleAction(RuleType.ACTION, ActionType.ACTION_BUY_BTC_DCA, BUY_USD_AMOUNT_PER_DAY, COINBASE_PRO_MINIMUM_BTC_TRADE_THRESHOLD, PERCENT_OF_TIME_TO_DO_ACTION_WHEN_TRIGGERED);
        rules.add(action1);
        
        /*
            TO ADD
            
            -account 1 - ACTION withdraw X bitcoin (from coinbase pro) to coinbase account, threshold $20->bitcoin, max $100->bitcoin    
            -account 1 - ACTION transfer X USD from bank 1, threshold >= $50, max $100
        */
        
        
    }
    
    private void DoMainLoop()
    {
        running = true;
        
        while(running)
        {
            ExecuteRules();
            
            lsoc.library.utilities.Sleep.Sleep(CryptomoneyAutotask.iterationIntervalMS); //sleep X minutes
            //todo: this is slightly off because it might take more than 1 minute to run through the code before getting to this point, optionally use a timer instead
        }
        
    }
    
    private void ExecuteRules()
    {
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ALLOWANCE)
            {
                r.DoAction();
            }
        }
        
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ALARM)
            {
                r.DoAction();
            }
        }
        
        
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ACTION)
            {
                r.DoAction();
            }
        }
    }
    
    
}
