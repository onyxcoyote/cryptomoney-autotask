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
public class Autotask
{
    
    public ExchangeAccount account1; //for now it can only work on 1 account at a time, this works better with the coinbase pro library and is more secure anyway
    
    public boolean running = false;   
    
    public Rules rules = new Rules();
    public ArrayList<Rule> availableRules = new ArrayList<>();
    
    private boolean executeImmediately;
    
    private FiatSafetyLimits fiatSafetyLimitsQuantity = new FiatSafetyLimits();
    private CoinSafetyLimits coinSafetyLimitsQuantity = new CoinSafetyLimits();
    public CoinPriceLimits coinPriceLimitsValue = new CoinPriceLimits();
    private static double MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY = 5000; //limit set to prevent hitting the API too much
    


    //todo: add more restrictions: https://www.coinbase.com/legal/trading_rules
    
    public Autotask(boolean _executeImmediately)
    {
        //note: these limits may need to be adjusted by config
        fiatSafetyLimitsQuantity.addLimit(new FiatSafetyLimit(FiatCurrencyType.USD, 200)); //limit for sanity check
        fiatSafetyLimitsQuantity.addLimit(new FiatSafetyLimit(FiatCurrencyType.EUR, 175)); //limit for sanity check
        fiatSafetyLimitsQuantity.addLimit(new FiatSafetyLimit(FiatCurrencyType.GBP, 175)); //limit for sanity check
        
        //note: these limits may need to be adjusted by config
        coinSafetyLimitsQuantity.addLimit(new CoinSafetyLimit(CoinCurrencyType.BTC, 0.1)); //limit for sanity check
        coinSafetyLimitsQuantity.addLimit(new CoinSafetyLimit(CoinCurrencyType.ETH, 1.0)); //limit for sanity check
        
        //note: these limits may need to be adjusted by config since the realistic value can change over time
        coinPriceLimitsValue.addLimit(new CoinPriceLimit(CoinCurrencyType.BTC, FiatCurrencyType.USD, 1000, 17500));
        coinPriceLimitsValue.addLimit(new CoinPriceLimit(CoinCurrencyType.ETH, FiatCurrencyType.USD, 25, 500));
        coinPriceLimitsValue.addLimit(new CoinPriceLimit(CoinCurrencyType.LTC, FiatCurrencyType.USD, 10, 300));
        coinPriceLimitsValue.addLimit(new CoinPriceLimit(CoinCurrencyType.BCH, FiatCurrencyType.USD, 10, 300));
        
        account1 = new ExchangeAccount(); //todo: change this to "BTC"?  In case we have other types of accounts running the same API key (like ETH or LTC)
        executeImmediately = _executeImmediately;
        
        
    }
    
    /**
     * @return 
     */
    /*public OrderService orderService()
    { 
        return CryptomoneyAutotask.orderService;
    }*/
    
    public void Run()
    {
        Initialize();
        
        DoMainLoop();
    }
    
    private void Initialize()
    {
        LoadRuleHelp();
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
        
        //todo: https://docs.pro.coinbase.com/#get-products min BTC purchase size can change in the future, get the value dynamically
    }
    
    
    /**
     * Load all possible rule types.  This is used to validate actual rules.
     */
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
    
    private void LoadRules() throws java.io.IOException, Exception
    {
        //todo: use BigDecimal instead of double for calculations
        
        
        //ALARM RULES
        boolean ALARM_PRINT_BALANCE__enable = Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString("ALARM_PRINT_BALANCE__enable"));
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

        
      
        //ACTION RULES

        //account 1 - ACTION buy (w/ BUYMETHOD) around X bitcoin per day using USD
        for(int i=1;i<=20;i++)
        {        
            String prefix = "ACTION_BUY_COIN_DCA_POSTONLY_";
            String fullPrefix = prefix+i+"_";
            
            if(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable")==null)
            {
                break;
            }
            
            boolean ACTION_BUY_COIN_DCA_POSTONLY__enable = Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable"));        
            if(ACTION_BUY_COIN_DCA_POSTONLY__enable)
            {
                CoinCurrencyType coinCurrencyType = CoinCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"coin_currency_type"));
                FiatCurrencyType fiatCurrencyType = FiatCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"fiat_currency_type"));
              
                //check for duplicate rule
                if(true)
                {
                    Rule dupRule = this.rules.getRule(RuleType.ACTION, ActionType.ACTION_BUY_COIN_DCA_POSTONLY, fiatCurrencyType, coinCurrencyType);
                    if(dupRule != null)
                    {
                        throw new Exception("duplicate rule already exists " + dupRule.getHelpString());
                    }
                }
                    
                //ALLOWANCE RULE
                double ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity = Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"amountPerDayCurrencyQuantity"));
                if(ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity < 0 || ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"amountPerDayUS exceeds hard-coded safe maximum");
                }
                RuleAllowance_BuyCoin allowance_buyCoin = new RuleAllowance_BuyCoin(
                        coinCurrencyType, 
                        fiatCurrencyType, 
                        executeImmediately, 
                        ACTION_BUY_COIN_DCA_POSTONLY__amountPerDayCurrencyQuantity); 
                rules.add(allowance_buyCoin);

                //PROCESS ORDERS RULE
                double ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay = Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay"));
                if(ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay < 0 || ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
                {
                    throw new Exception(fullPrefix+"PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
                }            
                RuleAction_ProcessCoinBuyPostOrders action_processOrders = new RuleAction_ProcessCoinBuyPostOrders(
                        coinCurrencyType, 
                        fiatCurrencyType, 
                        executeImmediately, 
                        ACTION_BUY_COIN_DCA_POSTONLY__PROCESS_COIN_BUY_POST_ORDERS__maximumAvgOccurrencesPerDay);
                rules.add(action_processOrders);
                


                //BUY RULE
                double ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay =   Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"maximumAvgOccurrencesPerDay"));
                double ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity =         Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"minimumQuantityBuyCurrencyQuantity"));
                double ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold =  Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"minimumQuantityCoinThreshold"));
                double ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin =           Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"maximumQuantityCoin"));
                double ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed =         Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"randomChanceToProceed")); //allows some randomness so it doesn't always happen so predictably


                if(ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay < 0 || ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
                {
                    throw new Exception(fullPrefix+"maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
                }      
                if(ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity < 1 || ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"minimumQuantityBuyCurrencyQuantity exceeds hard-coded safe maximum");
                }      
                if(ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold < 0.0001 || ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold > this.coinSafetyLimitsQuantity.getLimit(coinCurrencyType))
                {
                    throw new Exception(fullPrefix+"minimumQuantityCoinThreshold exceeds hard-coded safe maximum");
                }      
                if(ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin < 0.001 || ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin > this.coinSafetyLimitsQuantity.getLimit(coinCurrencyType))
                {
                    throw new Exception(fullPrefix+"maximumQuantityCoin exceeds hard-coded safe maximum");
                }      
                if(ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed < 0.01 || ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed > 1.00)
                {
                    throw new Exception(fullPrefix+"randomChanceToProceed exceeds hard-coded safe maximum");
                }          

                RuleAction_BuyCoinDCAPostOnly action_buy = new RuleAction_BuyCoinDCAPostOnly(
                        coinCurrencyType,
                        fiatCurrencyType,
                        executeImmediately,
                        ACTION_BUY_COIN_DCA_POSTONLY__maximumAvgOccurrencesPerDay, 
                        ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityBuyCurrencyQuantity,
                        ACTION_BUY_COIN_DCA_POSTONLY__minimumQuantityCoinThreshold, 
                        ACTION_BUY_COIN_DCA_POSTONLY__maximumQuantityCoin,
                        ACTION_BUY_COIN_DCA_POSTONLY__randomChanceToProceed);
                rules.add(action_buy);
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("rule disabled: " + fullPrefix);
            }            
        }
        
        
        //account 1 - ACTION withdraw X coin (from coinbase pro) to coinbase account 
        for(int i=1;i<=20;i++)
        {
            String prefix = "ACTION_WITHDRAW_COIN_TO_COINBASE_";
            String fullPrefix = prefix+i+"_";
            
            if(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable")==null)
            {
                break;
            }
            
            boolean ACTION_WITHDRAW_COIN_TO_COINBASE__enable = Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable"));        
            if(ACTION_WITHDRAW_COIN_TO_COINBASE__enable)
            {
                CoinCurrencyType coinCurrencyType = CoinCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"coin_currency_type"));
                FiatCurrencyType fiatCurrencyType = FiatCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"fiat_currency_type"));

                //check for duplicate rule
                if(true)
                {
                    Rule dupRule = this.rules.getRule(RuleType.ACTION, ActionType.ACTION_WITHDRAW_COIN_TO_COINBASE, fiatCurrencyType, coinCurrencyType);
                    if(dupRule != null)
                    {
                        throw new Exception("duplicate rule already exists " + dupRule.getHelpString());
                    }
                }                
                
                //ALLOWANCE RULE            
                double ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity = Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"amountPerDayCurrencyQuantity"));
                if(ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"amountPerDayCurrencyQuantity exceeds hard-coded safe maximum");
                }
                RuleAllowance_WithdrawCoinToCoinbase allowanceWithdrawCointoCoinbase = new RuleAllowance_WithdrawCoinToCoinbase(
                        coinCurrencyType,
                        fiatCurrencyType, 
                        executeImmediately, 
                        ACTION_WITHDRAW_COIN_TO_COINBASE__amountPerDayCurrencyQuantity);
                rules.add(allowanceWithdrawCointoCoinbase);


                //ACTION - WITHDRAW RULE
                double ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay =    Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"maximumAvgOccurrencesPerDay"));
                double ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold =     Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"minimumCurrencyQuantityThreshold"));
                double ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity =     Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"maximumCurrencyQuantity"));

                if(ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
                {
                    throw new Exception(fullPrefix+"maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
                }
                if(ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__minimumCurrencyQuantityThreshold > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"minimumCurrencyQuantityThreshold exceeds hard-coded safe maximum");
                }
                if(ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity < 0 || ACTION_WITHDRAW_COIN_TO_COINBASE__maximumCurrencyQuantity > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"maximumCurrencyQuantity exceeds hard-coded safe maximum");
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
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("rule disabled: " + fullPrefix);
            }
        }

        
        //account 1 - ACTION transfer X USD from bank 1
        for(int i=1;i<=20;i++)
        {        
            String prefix = "ACTION_DEPOSIT_FIAT_";
            String fullPrefix = prefix+i+"_";
            
            if(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable")==null)
            {
                break;
            }
            
            boolean ACTION_DEPOSIT_FIAT__enable = Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable"));        
            if(ACTION_DEPOSIT_FIAT__enable)
            {

                FiatCurrencyType fiatCurrencyType = FiatCurrencyType.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"fiat_currency_type"));

                /*  Allow duplicates for this rule
                //check for duplicate rule
                if(true)
                {
                    Rule dupRule = this.rules.getRule(RuleType.ACTION, ActionType.ACTION_DEPOSIT_FIAT, fiatCurrencyType);
                    if(dupRule != null)
                    {
                        throw new Exception("duplicate rule already exists " + dupRule.getHelpString());
                    }
                }*/               
                
                //ALLOWANCE RULE            
                double ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity  = Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"amountPerDayCurrencyQuantity"));
                if(ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity < 0 || ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"amountPerDayCurrencyQuantity exceeds hard-coded safe maximum");
                }
                RuleAllowance_DepositFiat allowanceDepositFiat = new RuleAllowance_DepositFiat(
                        fiatCurrencyType,
                        executeImmediately, 
                        ACTION_DEPOSIT_FIAT__amountPerDayCurrencyQuantity);
                rules.add(allowanceDepositFiat);


                //ACTION - DEPOSIT RULE
                double ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay =    Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"maximumAvgOccurrencesPerDay"));
                double ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold =      Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"minimumCurrencyQuantityThreshold"));
                double ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity =      Double.parseDouble(CryptomoneyAutotask.config.getConfigString(fullPrefix+"maximumCurrencyQuantity"));

                if(ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay < 0 || ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay > MAXIMUM_SAFE_NUMBER_OF_EXECUTIONS_PER_DAY)
                {
                    throw new Exception(fullPrefix+"maximumAvgOccurrencesPerDay exceeds hard-coded safe maximum");
                }            
                if(ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold < 0 || ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"minimumCurrencyQuantityThreshold exceeds hard-coded safe maximum");
                }
                if(ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity < 0 || ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity > this.fiatSafetyLimitsQuantity.getLimit(fiatCurrencyType))
                {
                    throw new Exception(fullPrefix+"maximumCurrencyQuantity exceeds hard-coded safe maximum");
                }

                RuleAction_DepositFiat action3 = new RuleAction_DepositFiat(
                        fiatCurrencyType,
                        executeImmediately,
                        ACTION_DEPOSIT_FIAT__maximumAvgOccurrencesPerDay, 
                        ACTION_DEPOSIT_FIAT__minimumCurrencyQuantityThreshold, 
                        ACTION_DEPOSIT_FIAT__maximumCurrencyQuantity);
                rules.add(action3);
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("rule disabled: " + fullPrefix);
            }            
        }
        
        //Additional coinbase wallets to include in alarm print
        for(int i=1;i<=20;i++)
        {        
            String prefix = "ALARM_PRINT_BALANCE_INCLUDE_COINBASE_WALLET_";
            String fullPrefix = prefix+i+"_";
            
            //CryptomoneyAutotask.logProv.LogMessage("checking rule (one-time): " + fullPrefix);   
            
            if(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable")==null)
            {
                break;
            }
            
            boolean ACTION_DEPOSIT_FIAT__enable = Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable"));        
            if(ACTION_DEPOSIT_FIAT__enable)
            {
                CryptomoneyAutotask.logProv.LogMessage("applying rule (one-time): " + fullPrefix);    
                
                //doesn't add a rule, just caches the account
                WalletAccountCurrency walletAccountCurrencyType = WalletAccountCurrency.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"currency_type"));
                
                String wallet = account1.getCoinbaseRegularAccount_Id(walletAccountCurrencyType);
                lsoc.library.utilities.Sleep.Sleep(200); //throttle to prevent too quick API calls
                
                if(wallet != null && wallet != "")
                {
                    CryptomoneyAutotask.logProv.LogMessage("coinbase wallet cached: " + wallet);    
                }
                else
                {
                    CryptomoneyAutotask.logProv.LogMessage("coinbase wallet not found: " + walletAccountCurrencyType.toString());    
                }
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("rule disabled: " + fullPrefix);
            }              
        }
        
        //Additional coinbase PRO wallets to include in alarm print
        for(int i=1;i<=20;i++)
        {        
            String prefix = "ALARM_PRINT_BALANCE_INCLUDE_COINBASE_PRO_WALLET_";
            String fullPrefix = prefix+i+"_";
            
            if(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable")==null)
            {
                break;
            }
            
            boolean ACTION_DEPOSIT_FIAT__enable = Boolean.parseBoolean(CryptomoneyAutotask.config.getConfigString(fullPrefix+"enable"));        
            if(ACTION_DEPOSIT_FIAT__enable)
            {
                CryptomoneyAutotask.logProv.LogMessage("applying rule (one-time): " + fullPrefix);    
                
                //doesn't add a rule, just caches the account
                WalletAccountCurrency walletAccountCurrencyType = WalletAccountCurrency.valueOf(CryptomoneyAutotask.config.getConfigString(fullPrefix+"currency_type"));
                
                Account wallet = account1.getCoinbaseProWalletAccount(walletAccountCurrencyType);
                lsoc.library.utilities.Sleep.Sleep(200); //throttle to prevent too quick API calls
                
                if(wallet != null)
                {
                    CryptomoneyAutotask.logProv.LogMessage("coinbase PRO wallet cached: " + wallet.getId());    
                }
                else
                {
                    CryptomoneyAutotask.logProv.LogMessage("coinbase PRO wallet not found: " + walletAccountCurrencyType.toString());    
                }
            }
            else
            {
                CryptomoneyAutotask.logProv.LogMessage("rule disabled: " + fullPrefix);
            }              
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

    /**
     * Main loop
     */
    private void DoMainLoop()
    {
        running = true;
        
        while(running)
        {
            ExecuteRules();
            
            lsoc.library.utilities.Sleep.Sleep(CryptomoneyAutotask.iterationIntervalMS); //sleep X milli-seconds
            //todo: this is slightly off because it might take more than 1 minute to run through the code before getting to this point, use a timer instead if more exact timing is important
        }
        
    }
    
    private void ExecuteRules()
    {
        CryptomoneyAutotask.logProv.LogMessage("-----");
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ALLOWANCE)
            {
                wrapAction(r);               
            }
        }
        
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ALARM)
            {
                wrapAction(r);
            }
        }
        
        
        for(Rule r : rules)
        {
            if(r.getRuleType() == RuleType.ACTION)
            {
                wrapAction(r);
            }
        }
    }
    
    private void wrapAction(Rule rule)
    {
        try
        {
            rule.doAction();
        }
        catch(Exception ex)
        {
            if(ex.getMessage().contains("I/O error on GET request")) //coinbase error
            {
                CryptomoneyAutotask.logMultiplexer.LogException(ex);
                lsoc.library.utilities.Sleep.Sleep(1000*60*2); //sleep 5 minutes after an error, let the rule not complete and move on
            }
            else
            {
                //an unexpected error, for now exit.  If this happens, handle it better depending on the type of error and desired result.
                CryptomoneyAutotask.logMultiplexer.LogException(ex);
                System.exit(1); 
            }
        }      
    }
    
    
}
