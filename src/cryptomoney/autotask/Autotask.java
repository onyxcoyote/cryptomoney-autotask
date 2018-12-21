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
        account1 = new ExchangeAccount();
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
        Initialize();
        
        CancelOpenOrders();
        
        DoMainLoop();
    }
    
    private void Initialize()
    {
        LoadRuleHelp();
        LoadAccounts();
        LoadRules();
        ValidateRules();
        
        //todo-low: https://docs.pro.coinbase.com/#get-products min size can change in the future
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
        
        availableRules.add(new RuleAllowance_BuyBTC());
        availableRules.add(new RuleAllowance_DepositUSD());
        availableRules.add(new RuleAllowance_WithdrawBTCToCoinbase());
              
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
        
        //ALLOWANCE RULES
        //account 1 - ALLOWANCE  buy_bitcoin $21/day (divided per hour)
        double ALLOWANCE_USD_AMOUNT_PER_DAY = 16000.00; //TODO: test
        //double ALLOWANCE_USD_AMOUNT_PER_DAY = 21.00;
        RuleAllowance_BuyBTC allowance1 = new RuleAllowance_BuyBTC(executeImmediately, ALLOWANCE_USD_AMOUNT_PER_DAY); 
        rules.add(allowance1);
        
        //account 1 - ALLOWANCE  coinbase_pro_to_coinbase $20/day (divided per hour)
        double ALLOWANCE_BTC_TO_COINBASE_PER_DAY = 14000.00;
        //double ALLOWANCE_BTC_TO_COINBASE_PER_DAY = 20.50;
        RuleAllowance_WithdrawBTCToCoinbase allowance2 = new RuleAllowance_WithdrawBTCToCoinbase(executeImmediately, ALLOWANCE_BTC_TO_COINBASE_PER_DAY);
        rules.add(allowance2);
        
        //account 1 - ALLOWANCE  deposit USD $22/day (divided per hour)
        double ALLOWANCE_DEPOSIT_USD_PER_DAY = 95000.00;
        //double ALLOWANCE_DEPOSIT_USD_PER_DAY = 22.00;
        RuleAllowance_DepositUSD allowance3 = new RuleAllowance_DepositUSD(executeImmediately, ALLOWANCE_DEPOSIT_USD_PER_DAY);
        rules.add(allowance3);

        
        
        //ALARM RULES
        /*
            TO ADD
            -account 1 - ALARM if buy_btc_allowance > $20 and USD balance < $21
            -account 1 - ALARM if transfer_btc_coinbase_pro_to_coinbase_allowance > $20 and BTC balance < $21->bitcoin
            -account 1 - ALARM if coinbase_btc_balance <  $50->BTC, max once per day?
        */
        //todo:
        
        
        /*
        //ACTION RULES
        //account 1 - ACTION buy (w/ BUYMETHOD) around X bitcoin per day using USD, threshold 0.001 bitcoin, max .005 bitcoin, Do 25% of the time (randomness)
        //todo: add max single purchase? 
        //double BUY_USD_AMOUNT_PER_DAY_USD = 21.00;
        double MAX_AVERAGE_ACTIONS_PER_DAY = 10;
        double MIN_USD_BUY_AMOUNT = 8.00;
        
        double COINBASE_PRO_MINIMUM_BTC_TRADE_THRESHOLD = 0.001;
        double MAXIMUM_BTC_AMOUNT_TO_PURCHASE = 0.01;
        double PERCENT_OF_TIME_TO_DO_ACTION_WHEN_TRIGGERED = 0.25; //allows some randomness so it doesn't always happen
        RuleAction_BuyBTCDCAPostOnly action1 = new RuleAction_BuyBTCDCAPostOnly(
                executeImmediately,
                MAX_AVERAGE_ACTIONS_PER_DAY, 
                MIN_USD_BUY_AMOUNT,
                COINBASE_PRO_MINIMUM_BTC_TRADE_THRESHOLD, 
                MAXIMUM_BTC_AMOUNT_TO_PURCHASE,
                PERCENT_OF_TIME_TO_DO_ACTION_WHEN_TRIGGERED);
        rules.add(action1);

        //todo: get rid of allowance and add to regular rules
        
        //account 1 - ACTION withdraw X bitcoin (from coinbase pro) to coinbase account, threshold $20->bitcoin, max $100->bitcoin    
        //double TRANSFER_TO_COINBASE_PER_DAY_USD = 20.00;
        MAX_AVERAGE_ACTIONS_PER_DAY = 1.0;
        double MINIMUM_AMOUNT_TO_TRANSFER = 20.00;
        double MAXIMUM_AMOUNT_TO_TRANSFER = 50.00;
        RuleAction_WithdrawBTCToCoinbase action2 = new RuleAction_WithdrawBTCToCoinbase(
                executeImmediately,
                MAX_AVERAGE_ACTIONS_PER_DAY,
                MINIMUM_AMOUNT_TO_TRANSFER,
                MAXIMUM_AMOUNT_TO_TRANSFER);
        rules.add(action2);
        */

        //account 1 - ACTION transfer X USD from bank 1, threshold >= $50, max $100
        //double DEPOSIT_USD_PER_DAY = 22.00;
        double MAX_AVERAGE_ACTIONS_PER_DAY = 0.5;
        
        double MINIMUM_AMOUNT_TO_DEPOSIT = 50.00;
        double MAXIMUM_AMOUNT_TO_DEPOSIT = 100.00;
        RuleAction_DepositUSD action3 = new RuleAction_DepositUSD(
                executeImmediately,
                MAX_AVERAGE_ACTIONS_PER_DAY, 
                MINIMUM_AMOUNT_TO_DEPOSIT, 
                MAXIMUM_AMOUNT_TO_DEPOSIT);
        rules.add(action3);
        
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
                    valid = true;
                }
            }
            
            if(!valid)
            {
                CryptomoneyAutotask.logProv.LogMessage("rule not found " + rule.getRuleType() + " " + rule.getActionType());     
                CryptomoneyAutotask.logProv.LogMessage("rule validation failed");     
                System.exit(1);
            }
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
