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



import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.accounts.Account;

import cryptomoney.autotask.exchangeaccount.ExchangeAccount;
import cryptomoney.autotask.currency.*;
import cryptomoney.autotask.exchangeaccount.WalletAccountCurrency;
import cryptomoney.autotask.rule.Rule;
import cryptomoney.autotask.rule.RuleAction_BuyCoinDCAPostOnly;
import cryptomoney.autotask.rule.RuleAllowance_WithdrawCoinToCoinbase;
import cryptomoney.autotask.rule.RuleType;
import cryptomoney.autotask.rule.*;

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
    
    private FiatSafetyLimits fiatSafetyLimits = new FiatSafetyLimits();
    private CoinSafetyLimits coinSafetyLimits = new CoinSafetyLimits();
    private static double MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY = 5000; //limit set to prevent hitting the API too much
    
    //public static int iterationIntervalMS = 1000*60*5; //5 minutes
    

    
    //public ILoggingProvider logProv = new LoggingProviderSimple();
  
    /*public void run(String[] args) throws Exception 
    {
        
    }*/
    
    
    //@Autowired
    public Autotask(boolean _executeImmediately)
    {
        fiatSafetyLimits.addLimit(new FiatSafetyLimit(FiatCurrencyType.USD, 200)); //limit for sanity check
        coinSafetyLimits.addLimit(new CoinSafetyLimit(CoinCurrencyType.BTC, 0.1)); //limit for sanity check
        
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
        Initialize(account1); //TODO: credentials/exchange info should probably tie to account instead of being static params
        
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
        TestAccountAPI();
        
        //todo: https://docs.pro.coinbase.com/#get-products min BTC purchase size can change in the future
    }
    
    /*private void CancelOpenOrders()
    {
        account1.ProcessBTCBuyOrders(true);
    }*/
    
        
    private void LoadRuleHelp()
    {
        availableRules.add(new RuleAction_BuyCoinDCAPostOnly());
        availableRules.add(new RuleAction_DepositFiat());
        availableRules.add(new RuleAction_WithdrawCoinToCoinbase());
        
        availableRules.add(new RuleAction_ProcessCoinBuyPostOrders());
        
        availableRules.add(new RuleAllowance_BuyCoin());
        availableRules.add(new RuleAllowance_DepositFiat());
        availableRules.add(new RuleAllowance_WithdrawCoinToCoinbase());
        
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
        
        
        //boolean ALLOWANCE_BUY_BTC__enable                   =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_BUY_BTC__enable"));
        //boolean ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__enable  =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_WITHDRAW_BTC_TO_COINBASE__enable"));
        //boolean ALLOWANCE_DEPOSIT_USD__enable               =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALLOWANCE_DEPOSIT_USD__enable"));
        //boolean ACTION_PROCESS_BTC_BUY_POST_ORDERS__enable  =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_PROCESS_BTC_BUY_POST_ORDERS__enable"));
        boolean ACTION_BUY_COIN_DCA_POSTONLY__enable         =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__enable"));
        boolean ACTION_WITHDRAW_COIN_TO_COINBASE__enable     =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__enable"));
        boolean ACTION_DEPOSIT_FIAT__enable                  =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_FIAT__enable"));
        boolean ALARM_PRINT_BALANCE__enable                 =   Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALARM_PRINT_BALANCE__enable"));
        
        
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

        //account 1 - ACTION buy (w/ BUYMETHOD) around X bitcoin per day using USD
        if(ACTION_BUY_COIN_DCA_POSTONLY__enable)
        {
            CoinCurrencyType coinCurrencyType = CoinCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__coin_currency_type"));
            FiatCurrencyType fiatCurrencyType = FiatCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__fiat_currency_type"));
            
            //ALLOWANCE RULE
            double ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity"));
            if(ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity < 0 || ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayUS exceeds hard-coded safe maximum");
            }
            RuleAllowance_BuyCoin allowance_buyBTC = new RuleAllowance_BuyCoin(
                    coinCurrencyType, 
                    fiatCurrencyType, 
                    executeImmediately, 
                    ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity); 
            rules.add(allowance_buyBTC);
            
            //PROCESS ORDERS RULE
            double ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay"));
            if(ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay < 0 || ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }            
            RuleAction_ProcessCoinBuyPostOrders action_processOrders = new RuleAction_ProcessCoinBuyPostOrders(
                    coinCurrencyType, 
                    fiatCurrencyType, 
                    executeImmediately, 
                    ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay);
            rules.add(action_processOrders);
            
            
            //BUY RULE
            double ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay =   Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay"));
            double ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity =         Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity"));
            double ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold =  Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold"));
            double ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin =           Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin"));
            double ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed =         Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed")); //allows some randomness so it doesn't always happen so predictably

            
            if(ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay < 0 || ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity < 1 || ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold < 0.0001 || ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold > this.coinSafetyLimits.getLimit(coinCurrencyType))
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin < 0.001 || ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin > this.coinSafetyLimits.getLimit(coinCurrencyType))
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin exceeds hard-coded safe maximum");
            }      
            if(ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed < 0.01 || ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed > 1.00)
            {
                throw new Exception("ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed exceeds hard-coded safe maximum");
            }          
                    
            RuleAction_BuyCoinDCAPostOnly action1 = new RuleAction_BuyCoinDCAPostOnly(
                    coinCurrencyType,
                    fiatCurrencyType,
                    executeImmediately,
                    ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay, 
                    ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity,
                    ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold, 
                    ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin,
                    ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed);
            rules.add(action1);
        }
        //todo: get rid of allowance and add to regular rules?
        
        
        //account 1 - ACTION withdraw X bitcoin (from coinbase pro) to coinbase account 
        if(ACTION_WITHDRAW_COIN_TO_COINBASE__enable)
        {
            CoinCurrencyType coinCurrencyType = CoinCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__coin_currency_type"));
            FiatCurrencyType fiatCurrencyType = FiatCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__fiat_currency_type"));

            //ALLOWANCE RULE            
            double ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity"));
            if(ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity exceeds hard-coded safe maximum");
            }
            RuleAllowance_WithdrawCoinToCoinbase allowanceBTCtoCoinbase = new RuleAllowance_WithdrawCoinToCoinbase(
                    coinCurrencyType,
                    fiatCurrencyType, 
                    executeImmediately, 
                    ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity);
            rules.add(allowanceBTCtoCoinbase);
            
            
            //ACTION - WITHDRAW RULE
            double ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay =    Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay"));
            double ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold =     Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold"));
            double ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity =     Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity"));
            
            if(ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }
            if(ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold exceeds hard-coded safe maximum");
            }
            if(ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity exceeds hard-coded safe maximum");
            }
            
            RuleAction_WithdrawCoinToCoinbase action2 = new RuleAction_WithdrawCoinToCoinbase(
                    coinCurrencyType,
                    fiatCurrencyType,                     
                    executeImmediately,
                    ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay,
                    ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold,
                    ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity);
            rules.add(action2);
        }

        
        //account 1 - ACTION transfer X USD from bank 1
        if(ACTION_DEPOSIT_FIAT__enable)
        {
            
            FiatCurrencyType fiatCurrencyType = FiatCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_FIAT__fiat_currency_type"));
            
            //ALLOWANCE RULE            
            double ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity  = Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity"));
            if(ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity < 0 || ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity exceeds hard-coded safe maximum");
            }
            RuleAllowance_DepositFiat allowanceDepositUSD = new RuleAllowance_DepositFiat(
                    fiatCurrencyType,
                    executeImmediately, 
                    ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity);
            rules.add(allowanceDepositUSD);
            
            
            //ACTION - DEPOSIT RULE
            double ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay =    Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay"));
            double ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold =      Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold"));
            double ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity =      Double.parseDouble(CryptomoneyAutotask.config.getConfigString("ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity"));
            
            if(ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay < 0 || ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
            {
                throw new Exception("ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
            }            
            if(ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold < 0 || ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold exceeds hard-coded safe maximum");
            }
            if(ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity < 0 || ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity > this.fiatSafetyLimits.getLimit(fiatCurrencyType))
            {
                throw new Exception("ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity exceeds hard-coded safe maximum");
            }
            
            RuleAction_DepositFiat action3 = new RuleAction_DepositFiat(
                    fiatCurrencyType,
                    executeImmediately,
                    ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay, 
                    ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold, 
                    ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity);
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
    
    private void TestAccountAPI()
    {
        if(CryptomoneyAutotask.accountService.getAccounts() == null) //API call
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
