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
    
    
    private static double MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY = 5000;
    private static double MAXIMUM_SAFE_VALUE_USD_TRANSACTION = 200;
    private static double MAXIMUM_SAFE_BTC_QUANTITY_TRANSACTION = 0.1;
    
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
        
        //CancelOpenOrders();
        
        DoMainLoop();
    }
    
    private void Initialize(ExchangeAccount _account)
    {
        LoadRuleHelp();
        LoadAccounts();
        try
        {
            LoadRules();
        }
        catch(Exception ex)
        {
            CryptomoneyAutotask.logMultiplexer.LogException(ex);
            System.exit(1);
        }
        ValidateRules();
        TestAccountAPI(_account);
        
        //todo: https://docs.pro.coinbase.com/#get-products min BTC purchase size can change in the future
    }
    
    /*private void CancelOpenOrders()
    {
        account1.ProcessBTCBuyOrders(true);
    }*/
    
        
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
    
    private void LoadRules() throws java.io.IOException, Exception
    {
        //todo: use BigDecimal instead of double for calculations
        
        
        
        
        boolean ALLOWANCE_BUY_BTC__enable                   =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_BUY_BTC__enable"));
        boolean ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__enable  =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__enable"));
        boolean ALLOWANCE_DEPOSIT_USD__enable               =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_DEPOSIT_USD__enable"));
        boolean ACTION_PROCESS_BTC_BUY_POST_ORDERS__enable  =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_PROCESS_BTC_BUY_POST_ORDERS__enable"));
        boolean ACTION_BUY_BTC_DCA_POSTONLY__enable         =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_BTC_DCA_POSTONLY__enable"));
        boolean ACTION_WITHDRAW_BTC_TO_COINBASE__enable     =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_BTC_TO_COINBASE__enable"));
        boolean ACTION_DEPOSIT_USD__enable                  =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_USD__enable"));
        boolean ALARM_PRINT_BALANCE__enable                 =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALARM_PRINT_BALANCE__enable"));
        
        
        //ALLOWANCE RULES
        
        //account 1 - ALLOWANCE  buy_bitcoin $21/day (divided per hour)
        if(ALLOWANCE_BUY_BTC__enable)
        {
            double ALLOWANCE_BUY_BTC__amountPerDayUS = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_BUY_BTC__amountPerDayUS"));
            if(ALLOWANCE_BUY_BTC__amountPerDayUS < 0 || ALLOWANCE_BUY_BTC__amountPerDayUS > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ALLOWANCE_BUY_BTC__amountPerDayUS exceeds hard-coded safe maximum");
            }
            RuleAllowance_BuyBTC allowance_buyBTC = new RuleAllowance_BuyBTC(executeImmediately, ALLOWANCE_BUY_BTC__amountPerDayUS); 
            rules.add(allowance_buyBTC);
        }
        
        //account 1 - ALLOWANCE  coinbase_pro_to_coinbase $20/day (divided per hour)
        if(ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__enable)
        {
            double ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__amountPerDayUSD = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__amountPerDayUSD"));
            if(ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__amountPerDayUSD < 0 || ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__amountPerDayUSD > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__amountPerDayUSD exceeds hard-coded safe maximum");
            }
            RuleAllowance_WithdrawBTCToCoinbase allowanceBTCtoCoinbase = new RuleAllowance_WithdrawBTCToCoinbase(executeImmediately, ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__amountPerDayUSD);
            rules.add(allowanceBTCtoCoinbase);
        }

        
        //account 1 - ALLOWANCE  deposit USD $22/day (divided per hour)
        if(ALLOWANCE_DEPOSIT_USD__enable)
        {
            double ALLOWANCE_DEPOSIT_USD__amountPerDayUSD  = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_DEPOSIT_USD__amountPerDayUSD"));
            if(ALLOWANCE_DEPOSIT_USD__amountPerDayUSD < 0 || ALLOWANCE_DEPOSIT_USD__amountPerDayUSD > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ALLOWANCE_DEPOSIT_USD__amountPerDayUSD exceeds hard-coded safe maximum");
            }
            RuleAllowance_DepositUSD allowanceDepositUSD = new RuleAllowance_DepositUSD(executeImmediately, ALLOWANCE_DEPOSIT_USD__amountPerDayUSD);
            rules.add(allowanceDepositUSD);
        }
        
        
        //ALARM RULES
        if(ALARM_PRINT_BALANCE__enable)
        {
            double ALARM_PRINT_BALANCE__maximumAvgOccurrencesPerDay = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ALARM_PRINT_BALANCE__maximumAvgOccurrencesPerDay"));
            if(ALARM_PRINT_BALANCE__maximumAvgOccurrencesPerDay < 0 || ALARM_PRINT_BALANCE__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ALARM_PRINT_BALANCE__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }
            RuleAlarm_PrintBalance alarm_printBalance = new RuleAlarm_PrintBalance(
                executeImmediately,
                ALARM_PRINT_BALANCE__maximumAvgOccurrencesPerDay);
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
        if(ACTION_PROCESS_BTC_BUY_POST_ORDERS__enable)
        {
            double ACTION_PROCESS_BTC_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_PROCESS_BTC_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay"));
            if(ACTION_PROCESS_BTC_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay < 0 || ACTION_PROCESS_BTC_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_PROCESS_BTC_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }            
            RuleAction_ProcessBTCBuyPostOrders action_processOrders = new RuleAction_ProcessBTCBuyPostOrders(
                executeImmediately, 
                ACTION_PROCESS_BTC_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay);
            rules.add(action_processOrders);
        }
        

        //account 1 - ACTION buy (w/ BUYMETHOD) around X bitcoin per day using USD
        if(ACTION_BUY_BTC_DCA_POSTONLY__enable)
        {
            
            double ACTION_BUY_BTC_DCA_POSTONLY__maximumAvgOccurrencesPerDay =   Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_BTC_DCA_POSTONLY__maximumAvgOccurrencesPerDay"));
            double ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityBuyUSD =         Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityBuyUSD"));
            double ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityCoinThreshold =  Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityCoinThreshold"));
            double ACTION_BUY_BTC_DCA_POSTONLY__maximumQuantityCoin =           Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_BTC_DCA_POSTONLY__maximumQuantityCoin"));
            double ACTION_BUY_BTC_DCA_POSTONLY__randomChanceToProceed =         Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_BTC_DCA_POSTONLY__randomChanceToProceed")); //allows some randomness so it doesn't always happen so predictably
            
            if(ACTION_BUY_BTC_DCA_POSTONLY__maximumAvgOccurrencesPerDay < 0 || ACTION_BUY_BTC_DCA_POSTONLY__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ACTION_BUY_BTC_DCA_POSTONLY__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityBuyUSD < 1 || ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityBuyUSD > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityBuyUSD exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityCoinThreshold < 0.0001 || ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityCoinThreshold > MAXIMUM_SAFE_BTC_QUANTITY_TRANSACTION)
            {
                throw new Exception("ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityCoinThreshold exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_BTC_DCA_POSTONLY__maximumQuantityCoin < 0.001 || ACTION_BUY_BTC_DCA_POSTONLY__maximumQuantityCoin > MAXIMUM_SAFE_BTC_QUANTITY_TRANSACTION)
            {
                throw new Exception("ACTION_BUY_BTC_DCA_POSTONLY__maximumQuantityCoin exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_BTC_DCA_POSTONLY__randomChanceToProceed < 0.01 || ACTION_BUY_BTC_DCA_POSTONLY__randomChanceToProceed > MAXIMUM_SAFE_BTC_QUANTITY_TRANSACTION)
            {
                throw new Exception("ACTION_BUY_BTC_DCA_POSTONLY__randomChanceToProceed exceeds hard-coded safe maximum");
            }          
                    
                    
            RuleAction_BuyBTCDCAPostOnly action1 = new RuleAction_BuyBTCDCAPostOnly(
                    executeImmediately,
                    ACTION_BUY_BTC_DCA_POSTONLY__maximumAvgOccurrencesPerDay, 
                    ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityBuyUSD,
                    ACTION_BUY_BTC_DCA_POSTONLY__minimumQuantityCoinThreshold, 
                    ACTION_BUY_BTC_DCA_POSTONLY__maximumQuantityCoin,
                    ACTION_BUY_BTC_DCA_POSTONLY__randomChanceToProceed);
            rules.add(action1);
        }
        //todo: get rid of allowance and add to regular rules?
        
        
        //account 1 - ACTION withdraw X bitcoin (from coinbase pro) to coinbase account 
        if(ACTION_WITHDRAW_BTC_TO_COINBASE__enable)
        {
            double ACTION_WITHDRAW_BTC_TO_COINBASE__maximumAvgOccurrencesPerDay =    Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_BTC_TO_COINBASE__maximumAvgOccurrencesPerDay"));
            double ACTION_WITHDRAW_BTC_TO_COINBASE__minimumUSDQuantityThreshold =     Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_BTC_TO_COINBASE__minimumUSDQuantityThreshold"));
            double ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity =     Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity"));
            
            if(ACTION_WITHDRAW_BTC_TO_COINBASE__maximumAvgOccurrencesPerDay < 0 || ACTION_WITHDRAW_BTC_TO_COINBASE__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_WITHDRAW_BTC_TO_COINBASE__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }
            if(ACTION_WITHDRAW_BTC_TO_COINBASE__minimumUSDQuantityThreshold < 0 || ACTION_WITHDRAW_BTC_TO_COINBASE__minimumUSDQuantityThreshold > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ACTION_WITHDRAW_BTC_TO_COINBASE__minimumUSDQuantityThreshold exceeds hard-coded safe maximum");
            }
            if(ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity < 0 || ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity exceeds hard-coded safe maximum");
            }
            
            RuleAction_WithdrawBTCToCoinbase action2 = new RuleAction_WithdrawBTCToCoinbase(
                    executeImmediately,
                    ACTION_WITHDRAW_BTC_TO_COINBASE__maximumAvgOccurrencesPerDay,
                    ACTION_WITHDRAW_BTC_TO_COINBASE__minimumUSDQuantityThreshold,
                    ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity);
            rules.add(action2);
        }

        
        //account 1 - ACTION transfer X USD from bank 1
        if(ACTION_DEPOSIT_USD__enable)
        {
            double ACTION_DEPOSIT_USD__maximumAvgOccurrencesPerDay =    Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_USD__maximumAvgOccurrencesPerDay"));
            double ACTION_DEPOSIT_USD__minimumUSDQuantityThreshold =      Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_USD__minimumUSDQuantityThreshold"));
            double ACTION_DEPOSIT_USD__maximumUSDQuantity =      Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_USD__maximumUSDQuantity"));
            
            if(ACTION_DEPOSIT_USD__maximumAvgOccurrencesPerDay < 0 || ACTION_DEPOSIT_USD__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_DEPOSIT_USD__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }            
            if(ACTION_DEPOSIT_USD__minimumUSDQuantityThreshold < 0 || ACTION_DEPOSIT_USD__minimumUSDQuantityThreshold > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity exceeds hard-coded safe maximum");
            }
            if(ACTION_DEPOSIT_USD__maximumUSDQuantity < 0 || ACTION_DEPOSIT_USD__maximumUSDQuantity > MAXIMUM_SAFE_VALUE_USD_TRANSACTION)
            {
                throw new Exception("ACTION_WITHDRAW_BTC_TO_COINBASE__maximumUSDQuantity exceeds hard-coded safe maximum");
            }
            
            RuleAction_DepositUSD action3 = new RuleAction_DepositUSD(
                    executeImmediately,
                    ACTION_DEPOSIT_USD__maximumAvgOccurrencesPerDay, 
                    ACTION_DEPOSIT_USD__minimumUSDQuantityThreshold, 
                    ACTION_DEPOSIT_USD__maximumUSDQuantity);
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
