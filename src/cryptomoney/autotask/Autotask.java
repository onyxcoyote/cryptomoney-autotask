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
import cryptomoney.autotask.rule.RuleAction_BuyBTCDCAPostOnly;
import cryptomoney.autotask.rule.RuleAllowance_WithdrawBTCToCoinbase;
import cryptomoney.autotask.rule.RuleType;
import cryptomoney.autotask.rule.*;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.accounts.Account;
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
    public ArrayList<Rule> availableRules = new ArrayList<>();
    
    private boolean executeImmediately;
    
    //public static int iterationIntervalMS = 1000*60*5; //5 minutes
    

    
    //public ILoggingProvider logProv = new LoggingProviderSimple();
  
    /*public void run(String[] args) throws Exception 
    {
        
    }*/
    
    
    //@Autowired
    public Autotask(boolean _executeImmediately)
    {
        account1 = new ExchangeAccount(); //todo: change this to "BTC"  In case we have other types of accounts running the same API key (like ETH or LTC)
        executeImmediately = _executeImmediately;
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
        Initialize(account1);
        
        CancelOpenOrders();
        
        DoMainLoop();
    }
    
    private void Initialize(ExchangeAccount _account)
    {
        LoadRuleHelp();
        LoadAccounts();
        LoadRules();
        ValidateRules();
        TestAccountAPI(_account);
        
        //todo: https://docs.pro.coinbase.com/#get-products min BTC purchase size can change in the future
    }
    
    private void CancelOpenOrders()
    {
        account1.ProcessBTCBuyOrders(true);
    }
    
        
    private void LoadRuleHelp()
    {
        availableRules.add(new RuleAction_BuyBTCDCAPostOnly());
        availableRules.add(new RuleAction_DepositUSD());
        availableRules.add(new RuleAction_WithdrawBTCToCoinbase());
        
        availableRules.add(new RuleAction_ProcessBTCBuyPostOrders());
        
        availableRules.add(new RuleAllowance_BuyBTC());
        availableRules.add(new RuleAllowance_DepositUSD());
        availableRules.add(new RuleAllowance_WithdrawBTCToCoinbase());
        
        availableRules.add(new RuleAlarm_PrintBalance());
              
        for(Rule ruleTemplate : availableRules)
        {
            CryptomoneyAutotask.logProv.LogMessage(ruleTemplate.getHelpString());
        }
    }
    
    private void LoadAccounts()
    {
        //accounts.put(1, account1); //account #1
    }
    
    private void LoadRules()
    {
        //todo: un-hard code rules
        //todo: use BigDecimal instead of double
        
        
        boolean useRule_RuleAllowance_BuyBTC                = true;
        boolean useRule_RuleAllowance_WithdrawBTCToCoinbase = true;
        boolean useRule_RuleAllowance_DepositUSD            = true;
        
        boolean useRule_RuleAction_ProcessBTCBuyPostOrders  = true;
        
        boolean useRule_RuleAction_BuyBTCDCAPostOnly        = true;
        boolean useRule_RuleAction_WithdrawBTCToCoinbase    = true;
        boolean useRule_RuleAction_DepositUSD               = true;
        
        boolean useRule_RuleAlarm_PrintBalance              = true;
        
        //ALLOWANCE RULES
        
        //account 1 - ALLOWANCE  buy_bitcoin $21/day (divided per hour)
        if(useRule_RuleAllowance_BuyBTC)
        {
            
            double ALLOWANCE_USD_BUY_AMOUNT_PER_DAY = 42.00; //TEST
            //double ALLOWANCE_USD_AMOUNT_PER_DAY = 21.00;
            RuleAllowance_BuyBTC allowance_buyBTC = new RuleAllowance_BuyBTC(executeImmediately, ALLOWANCE_USD_BUY_AMOUNT_PER_DAY); 
            rules.add(allowance_buyBTC);
        }
        
        //account 1 - ALLOWANCE  coinbase_pro_to_coinbase $20/day (divided per hour)
        if(useRule_RuleAllowance_WithdrawBTCToCoinbase)
        {
            double ALLOWANCE_BTC_TO_COINBASE_PER_DAY = 41.00; //TEST
            //double ALLOWANCE_BTC_TO_COINBASE_PER_DAY = 20.50;
            RuleAllowance_WithdrawBTCToCoinbase allowanceBTCtoCoinbase = new RuleAllowance_WithdrawBTCToCoinbase(executeImmediately, ALLOWANCE_BTC_TO_COINBASE_PER_DAY);
            rules.add(allowanceBTCtoCoinbase);
        }

        
        //account 1 - ALLOWANCE  deposit USD $22/day (divided per hour)
        if(useRule_RuleAllowance_DepositUSD)
        {
            double ALLOWANCE_DEPOSIT_USD_PER_DAY = 44.00; //TEST
            //double ALLOWANCE_DEPOSIT_USD_PER_DAY = 22.00;
            RuleAllowance_DepositUSD allowanceDepositUSD = new RuleAllowance_DepositUSD(executeImmediately, ALLOWANCE_DEPOSIT_USD_PER_DAY);
            rules.add(allowanceDepositUSD);
        }
        
        
        //ALARM RULES
        if(useRule_RuleAlarm_PrintBalance)
        {
            double MAX_AVERAGE_ACTIONS_PER_DAY = 4.0; //4 = every ~8 hours
            RuleAlarm_PrintBalance alarm_printBalance = new RuleAlarm_PrintBalance(
                executeImmediately,
                MAX_AVERAGE_ACTIONS_PER_DAY);
            rules.add(alarm_printBalance);
        }
        /*
            TO ADD
            -account 1 - ALARM if buy_btc_allowance > $20 and USD balance < $21
            -account 1 - ALARM if transfer_btc_coinbase_pro_to_coinbase_allowance > $20 and BTC balance < $21->bitcoin
            -account 1 - ALARM if coinbase_btc_balance <  $50->BTC, max once per day?
        */
        //todo:
        
        

        
      
        //ACTION RULES
        if(useRule_RuleAction_ProcessBTCBuyPostOrders)
        {
            double MAX_AVERAGE_ACTIONS_PER_DAY = 481.0; //480 = every ~3 minutes
            RuleAction_ProcessBTCBuyPostOrders action_processOrders = new RuleAction_ProcessBTCBuyPostOrders(
                executeImmediately, 
                MAX_AVERAGE_ACTIONS_PER_DAY);
            rules.add(action_processOrders);
        }
        

        //account 1 - ACTION buy (w/ BUYMETHOD) around X bitcoin per day using USD, threshold 0.001 bitcoin, max .005 bitcoin, Do 25% of the time (randomness)
        if(useRule_RuleAction_BuyBTCDCAPostOnly)
        {
            //double MAX_AVERAGE_ACTIONS_PER_DAY = 720.0; //stub
            //double MAX_AVERAGE_ACTIONS_PER_DAY = 48.0; //48 = every 30 minutes    CLOSE TO NORMAL TEST
            double MAX_AVERAGE_ACTIONS_PER_DAY = 1440.1; //TEST RANDOM CHANCE
            //MAX_AVERAGE_ACTIONS_PER_DAY = 10;

            double MIN_USD_BUY_AMOUNT = 5.00;
            double COINBASE_PRO_MINIMUM_BTC_TRADE_THRESHOLD = 0.001;
            double MAXIMUM_BTC_AMOUNT_TO_PURCHASE = 0.01;
            double PERCENT_OF_TIME_TO_DO_ACTION_WHEN_TRIGGERED = 0.15; //allows some randomness so it doesn't always happen
            RuleAction_BuyBTCDCAPostOnly action1 = new RuleAction_BuyBTCDCAPostOnly(
                    executeImmediately,
                    MAX_AVERAGE_ACTIONS_PER_DAY, 
                    MIN_USD_BUY_AMOUNT,
                    COINBASE_PRO_MINIMUM_BTC_TRADE_THRESHOLD, 
                    MAXIMUM_BTC_AMOUNT_TO_PURCHASE,
                    PERCENT_OF_TIME_TO_DO_ACTION_WHEN_TRIGGERED);
            rules.add(action1);
        }
        //todo: get rid of allowance and add to regular rules
        
        
        //account 1 - ACTION withdraw X bitcoin (from coinbase pro) to coinbase account, threshold $20->bitcoin, max $100->bitcoin    
        if(useRule_RuleAction_WithdrawBTCToCoinbase)
        {
            double MAX_AVERAGE_ACTIONS_PER_DAY = 24.1; //24 = every 60 minutes
            //MAX_AVERAGE_ACTIONS_PER_DAY = 1.0;

            //double MINIMUM_AMOUNT_TO_TRANSFER = 20.00; //STUB
            double MINIMUM_AMOUNT_TO_TRANSFER = 5.00; //TEST
            double MAXIMUM_AMOUNT_TO_TRANSFER = 50.00;
            RuleAction_WithdrawBTCToCoinbase action2 = new RuleAction_WithdrawBTCToCoinbase(
                    executeImmediately,
                    MAX_AVERAGE_ACTIONS_PER_DAY,
                    MINIMUM_AMOUNT_TO_TRANSFER,
                    MAXIMUM_AMOUNT_TO_TRANSFER);
            rules.add(action2);
        }

        
        //account 1 - ACTION transfer X USD from bank 1, threshold >= $50, max $100
        if(useRule_RuleAction_DepositUSD)
        {
            double MAX_AVERAGE_ACTIONS_PER_DAY = 4.1; //deposit 4x/day
            //MAX_AVERAGE_ACTIONS_PER_DAY = 0.5;

            double MINIMUM_AMOUNT_TO_DEPOSIT = 20.00; //TEST
            //double MINIMUM_AMOUNT_TO_DEPOSIT = 50.00;
            double MAXIMUM_AMOUNT_TO_DEPOSIT = 100.00;
            RuleAction_DepositUSD action3 = new RuleAction_DepositUSD(
                    executeImmediately,
                    MAX_AVERAGE_ACTIONS_PER_DAY, 
                    MINIMUM_AMOUNT_TO_DEPOSIT, 
                    MAXIMUM_AMOUNT_TO_DEPOSIT);
            rules.add(action3);
        }
        
        
        

        
    }
    
    private void ValidateRules()
    {
        for(Rule rule : rules)
        {
            boolean valid = false;
            for(Rule ruleTemplate : availableRules)
            {
                if(ruleTemplate.getRuleType().equals(rule.getRuleType()) &&
                        ruleTemplate.getActionType().equals(rule.getActionType()))
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("Rule added :"+ rule.getHelpString());
                    valid = true;
                }
            }
            
            if(!valid)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("rule not found " + rule.getRuleType() + " " + rule.getActionType());     
                CryptomoneyAutotask.logMultiplexer.LogMessage("rule validation failed");     
                System.exit(1);
            }
        }
        
    }
    
    private void TestAccountAPI(ExchangeAccount _acct)
    {
        Account testConnection = _acct.getCoinbaseProUSDAccount();
        if(testConnection == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to retrieve coinbase pro account, possible problem with authentication. See STDOUT.");  
            System.exit(1);
        }
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
                r.doAction();
            }
        }
        
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ALARM)
            {
                r.doAction();
            }
        }
        
        
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ACTION)
            {
                r.doAction();
            }
        }
    }
    
    
}
