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
package cryptomoney.autotask.exchangeaccount;



//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.orders.*;
import com.coinbase.exchange.api.entity.NewOrderSingle;
import com.coinbase.exchange.api.entity.Fill;
import com.coinbase.exchange.api.accounts.Account;
import com.coinbase.exchange.api.payments.CoinbaseAccount;
import com.coinbase.exchange.api.payments.PaymentType;


import cryptomoney.autotask.CryptomoneyAutotask;
import cryptomoney.autotask.exchangeaccount.*;
import cryptomoney.autotask.allowance.*;
import cryptomoney.autotask.currency.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.math.MathContext;
import java.math.RoundingMode;



import cryptomoney.autotask.functions.SharedFunctions;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
//@Component
public class ExchangeAccount
{
    
    
    public ExchangeType exchangeType = ExchangeType.CoinbasePro;
    
    
    public ArrayList<AllowanceFiat> allowancesFiat = new ArrayList<>();
    public ArrayList<AllowanceCoinFiat> allowancesCoinFiat = new ArrayList<>();
    public WalletAccountIDs walletAccountIDs = new WalletAccountIDs();
    
    private HashMap<String, Order> orders = new HashMap<>();
    
    public int btcBuyFrequencyDesperation = 0; //todo: can this be refactored, maybe make it an object
    public static final int BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD = 10;
    
    //private String coinbaseProUSDAccountId = null;
    //private String coinbaseProUSDBankPaymentTypeId = null;
    //private String coinbaseProBTCAccountId = null;
    //private String coinbaseRegularBTCAccountId = null;
    
    //private boolean has_coinbaseProUSDAccountId = false;
    //private boolean has_coinbaseProUSDBankPaymentTypeId = false;
    //private boolean has_coinbaseProBTCAccountId = false;
    //private boolean has_coinbaseRegularBTCAccountId = false;
    
    //private CoinCurrencyType coinCurrencyType;
    //private FiatCurrencyType fiatCurrencyType;
       
    public ExchangeAccount()
    {
        
        /*Allowance allowanceBuyBTCinUSD = new Allowance(); //TODO: choose currency type
        Allowance allowanceWithdrawBTCToCoinbaseInUSD = new Allowance(); //TODO: choose currency type
        AllowanceFiat allowanceDepositUSD = new AllowanceFiat();
        
        allowances.add(allowanceBuyBTCinUSD);
        allowances.add(allowanceWithdrawBTCToCoinbaseInUSD);
        allowancesFiat.add(allowanceDepositUSD);
        remove these*/
        
    }
    
    public AllowanceFiat getAllowanceFiat(AllowanceType _allowanceType, FiatCurrencyType _fiatCurrencyType)
    {
        for(AllowanceFiat allowance : allowancesFiat)
        {
            if(allowance.getAllowanceType().equals(_allowanceType) && 
                    allowance.getFiatCurrencyType().equals(_fiatCurrencyType))
            {
                return allowance;
            }
        }
        
        AllowanceFiat allowance = new AllowanceFiat(_allowanceType, _fiatCurrencyType);
        allowancesFiat.add(allowance);
        return allowance;
    }
    
    public AllowanceCoinFiat getAllowanceCoinFiat(AllowanceType _allowanceType, CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType)
    {
        for(AllowanceCoinFiat allowance : allowancesCoinFiat)
        {
            if(allowance.getAllowanceType().equals(_allowanceType) && 
                    allowance.getFiatCurrencyType().equals(_fiatCurrencyType) && 
                    allowance.getCoinCurrencyType().equals(_coinCurrencyType))
            {
                return allowance;
            }
        }
        
        AllowanceCoinFiat allowance = new AllowanceCoinFiat(_allowanceType, _coinCurrencyType, _fiatCurrencyType);
        allowancesCoinFiat.add(allowance);
        return allowance;
    }
    
    /*public ExchangeAccount(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType)
    {
        coinCurrencyType = _coinCurrencyType;
        fiatCurrencyType = _fiatCurrencyType;
    }*/
    
    public int getKnownPendingOrderCount()
    {
        if(orders == null)
        {
            return 0;
        }
        else
        {
            return orders.size();
        }
    }

    /**
     * This is needed for informational purposes only.  In most cases, it's only necessary to get the account_id.
     * @return 
     */
    public CoinbaseAccount getCoinbaseRegularAccount_ById(String _accountId)
    {
        List<CoinbaseAccount> coinbaseAccounts = CryptomoneyAutotask.paymentService.getCoinbaseAccounts(); //optional: instead of this get the id somehow else and code it into config?
        CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase accounts, count: "+coinbaseAccounts.size());

        CoinbaseAccount lookingForCoinbaseAccount = null;
        for(CoinbaseAccount coinbaseAccount : coinbaseAccounts)
        {
            CryptomoneyAutotask.logProv.LogMessage("coinbase account retrieved: " + coinbaseAccount.getId() + " " + 
                    coinbaseAccount.getCurrency() + " " + 
                    coinbaseAccount.getType() + " " + 
                    coinbaseAccount.getPrimary() + " " + 
                    coinbaseAccount.getBalance() + " " + 
                    coinbaseAccount.getName()
                    );
            if(coinbaseAccount.getId().equals(_accountId))
            {
                if(lookingForCoinbaseAccount != null)
                {
                    CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO ACCOUNTS FOUND WITH THE SAME ID WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                        //in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                    System.exit(1);
                }
                lookingForCoinbaseAccount = coinbaseAccount;
            }
        }

        if(lookingForCoinbaseAccount == null)
        {
            CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get coinbaseRegular Account. Exiting!");
            System.exit(1);
        }
        else
        {
            return lookingForCoinbaseAccount;
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getCoinbaseRegularBTCAccountById. Exiting!");
        System.exit(1);
        return null; 
    }
    
    /**
     * API CALL
     * @return 
     */
    public String getCoinbaseRegularAccount_Id(WalletAccountCurrency _walletAccountCurrency)
    {
        WalletAccountID walletAccountId = this.walletAccountIDs.getAccountID(WalletAccountType.CoinbaseRegularWallet, _walletAccountCurrency, false);
        
        if(walletAccountId.getAccount_Id() != null)
        {
            return walletAccountId.getAccount_Id();
        }
        else
        {
            List<CoinbaseAccount> coinbaseAccounts = CryptomoneyAutotask.paymentService.getCoinbaseAccounts(); //optional: instead of this get the id somehow else and code it into config?
            CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase accounts, count: "+coinbaseAccounts.size());

            CoinbaseAccount lookingForCoinbaseAccount = null;
            for(CoinbaseAccount coinbaseAccount : coinbaseAccounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("coinbase account retrieved: " + coinbaseAccount.getId() + " " + 
                        coinbaseAccount.getCurrency() + " " + 
                        coinbaseAccount.getType() + " " + 
                        coinbaseAccount.getPrimary() + " " + 
                        coinbaseAccount.getBalance() + " " + 
                        coinbaseAccount.getName()
                        );
                if(coinbaseAccount.getCurrency().equals(_walletAccountCurrency.toString()) && coinbaseAccount.getPrimary())
                {
                    if(lookingForCoinbaseAccount != null)
                    {
                        CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO -PRIMARY- ACCOUNTS OF TYPE " + _walletAccountCurrency.toString() + " FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                            //in this case, I think there COULD be multiple accounts unless the getPrimary() takes care of it
                        System.exit(1);
                    }
                    lookingForCoinbaseAccount = coinbaseAccount;
                }
            }
            
            if(lookingForCoinbaseAccount == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get coinbaseRegularAccount Account_Id. Exiting!");
                System.exit(1);
            }
            else
            {
                
                try
                {
                    walletAccountId.setAccount_Id(lookingForCoinbaseAccount.getId());
                }
                catch(Exception ex)
                {
                    CryptomoneyAutotask.logMultiplexer.LogException(ex);
                    System.exit(1);
                }
                return walletAccountId.getAccount_Id();
            }
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getCoinbaseRegularBTCAccount_Id. Exiting!");
        System.exit(1);
        return null;        
    }
    
    /**
     * get unique id representing bank account used to fund coinbase pro
     * Sometimes API call
     * @return 
     */
    public String getCoinbaseProPaymentType_AccountId(WalletAccountCurrency _walletAccountCurrency)
    {
        WalletAccountID walletAccountId = this.walletAccountIDs.getAccountID(WalletAccountType.CoinbaseProPaymentType, _walletAccountCurrency, false);
        
        if(walletAccountId.getAccount_Id() != null)
        {
            return walletAccountId.getAccount_Id(); //only this info is needed usually
        }
        else
        {

            List<PaymentType> paymentTypes = CryptomoneyAutotask.paymentService.getPaymentTypes();

            PaymentType paymentTypeBank = null;
            for(PaymentType paymentType : paymentTypes)
            {
                CryptomoneyAutotask.logProv.LogMessage("paymentType account retrieved: " + paymentType.getAllow_buy() + " " + paymentType.getId() + " " + paymentType.getName() + " " + paymentType.getType()); //todo: keep this, shows last 4 digit bank#?
                if(paymentType.getCurrency().equals(_walletAccountCurrency.toString()) && paymentType.getPrimary_buy())
                {
                    if(paymentTypeBank != null)
                    {
                        CryptomoneyAutotask.logProv.LogMessage("ERROR, TWO -PRIMARY BUY- PAYMENT TYPE " + _walletAccountCurrency.toString() + " ACCOUNTS FOUND WHEN EXPECTINE ONE, EXITING"); //todo: test this to make sure there would only be one account
                        System.exit(1);
                    }
                    paymentTypeBank = paymentType;
                }
            }


            if(paymentTypeBank == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get getCoinbasePro PaymentType_Id. Exiting!");
                System.exit(1);
            }
            else
            {
                try
                {
                    walletAccountId.setAccount_Id(paymentTypeBank.getId());
                }
                catch(Exception ex)
                {
                    CryptomoneyAutotask.logMultiplexer.LogException(ex);
                    System.exit(1);
                }
                return walletAccountId.getAccount_Id();
            }
        }
        
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation coinbasePro payment type. Exiting!");
        System.exit(1);
        return null;
    }
    
    public Account getCoinbaseProWalletAccount(WalletAccountCurrency _walletAccountCurrency)
    {
        WalletAccountID walletAccountId = this.walletAccountIDs.getAccountID(WalletAccountType.CoinbaseProWallet, _walletAccountCurrency, false);
        
        if(walletAccountId.getAccount_Id() != null)
        {
            Account acct = CryptomoneyAutotask.accountService.getAccount(walletAccountId.getAccount_Id());
            return acct;
        }
        else
        {
            List<Account> accounts = CryptomoneyAutotask.accountService.getAccounts();
            CryptomoneyAutotask.logProv.LogMessage("retrieved coinbase PRO accounts, count: "+accounts.size());

            Account lookingForAccount = null;
            for(Account acct : accounts)
            {
                CryptomoneyAutotask.logProv.LogMessage("CoinbasePro account retrieved: " + acct.getId() + " " + acct.getCurrency() + " " + acct.getAvailable() + "/" + acct.getBalance());
                if(acct.getCurrency().equals(_walletAccountCurrency.toString()))
                {
                    if(lookingForAccount != null)
                    {
                        CryptomoneyAutotask.logMultiplexer.LogMessage("ERROR, TWO " + _walletAccountCurrency.toString() + " ACCOUNTS FOUND WHEN EXPECTING ONE, EXITING"); //todo: test this to make sure there would only be one account
                        System.exit(1);
                    }
                    lookingForAccount = acct;
                }
            }
            
            if(lookingForAccount == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogMessage("Unable to get getCoinbasePro Account. Exiting!");
                System.exit(1);
            }
            else
            {
                try
                {
                    walletAccountId.setAccount_Id(lookingForAccount.getId());
                }
                catch(Exception ex)
                {
                    CryptomoneyAutotask.logMultiplexer.LogException(ex);
                    System.exit(1);
                }
                return lookingForAccount;
            }
        }
                
        CryptomoneyAutotask.logMultiplexer.LogMessage("Impossible situation getCoinbasePro Account. Exiting!");
        System.exit(1);
        return null;
    }

    
    //todo: move this elsewhere
    //todo: need to test successful fills
    /**
     * CALLS API
     */
    public boolean ProcessBuyOrders(CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType, boolean cancelAnyOpen) //TODO: change this to only look at buy orders
    {
        boolean madeChanges = false;
        
        if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();        
            
            List<Order> openOrders = CryptomoneyAutotask.app.orderService().getOpenOrders();
            ArrayList<Order> orphanedOrders = new ArrayList<>();
            ArrayList<Order> ordersNotOpen = new ArrayList<>();
            ArrayList<Order> ordersToCancel = new ArrayList<>();
            
            CryptomoneyAutotask.logProv.LogMessage("# open orders: " + openOrders.size());
            for(Order openOrder : openOrders)
            {
                boolean found = false;
                
                for(Order knownOrder : this.orders.values())
                {
                    if(knownOrder.getId().equals(openOrder.getId()))
                    {
                        //we know about order
                        found = true;
                        break;
                    }
                }
                
                if(!found)
                {    
                    double filledSize = Double.valueOf(openOrder.getFilled_size());
                    double originalSize = Double.valueOf(openOrder.getSize());
                    CryptomoneyAutotask.logProv.LogMessage("order ORPHAN ~pending: " + openOrder.getId() + " " + openOrder.toString() + " filled: " + filledSize + "/" + originalSize);
                    orphanedOrders.add(openOrder);
                    if(cancelAnyOpen)
                    {
                        if(openOrder.getProduct_id().equals(_coinCurrencyType.toString())) //only cancel/check orders of the specified coin type
                        {
                            CryptomoneyAutotask.logProv.LogMessage("Adding orphan order to cancellation queue: " + openOrder.getId());
                            ordersToCancel.add(openOrder);
                        }
                    }
                }
            }
            
            
            //see if our orders are still open
            CryptomoneyAutotask.logProv.LogMessage("checking order status...");
            for(Order knownOrder : this.orders.values())
            {
                boolean found = false;
                for(Order openOrder : openOrders)
                {
                    if(knownOrder.getId().equals(openOrder.getId()))
                    {

                            //order is still open
                            found = true;
                            if(cancelAnyOpen)
                            {
                                if(knownOrder.getProduct_id().equals(_coinCurrencyType.toString())) //only cancel/check orders of the specified coin type
                                {
                                    CryptomoneyAutotask.logProv.LogMessage("Adding known order to cancellation queue: " + openOrder.getId());
                                    ordersToCancel.add(openOrder);
                                }
                            }

                            double filledSize = Double.valueOf(openOrder.getFilled_size());
                            double originalSize = Double.valueOf(openOrder.getSize());
                            CryptomoneyAutotask.logProv.LogMessage("order known ~pending: " + openOrder.getId() + " " + openOrder.toString() + " filled: " + filledSize + "/" + originalSize);
                            //Cbpdca.logProv.LogMessage(openOrder.toString());
                            break;
                        }
                }
                
                if(!found)
                {    
                    ordersNotOpen.add(knownOrder);
                }
            }
            
            //CANCEL SPECIFIED ORDERS
            if(cancelAnyOpen)
            {
                if(ordersToCancel.size() > 0)
                {
                    CryptomoneyAutotask.logProv.LogMessage("cancelling orders, count: " + ordersToCancel.size());
                
                    for(Order orderToCancel : ordersToCancel)
                    {
                        lsoc.library.utilities.Sleep.Sleep(100); //slow down so we're not querying too fast. this limits it to 10x/second

                        CryptomoneyAutotask.logProv.LogMessage("submitting cancel for " + orderToCancel.getId());
                        String response = CryptomoneyAutotask.app.orderService().cancelOrder(orderToCancel.getId()); //API CALL
                        CryptomoneyAutotask.logMultiplexer.LogMessage("Requested order cancel, response: " + response);
                        //orders.remove(orderToCancel.getId()); //wait until later to verify it's been cancelled ? Or mark it as something we're cancelling?
                        ordersNotOpen.add(orderToCancel); //assume the cancel was complete

                        this.btcBuyFrequencyDesperation++;
                        CryptomoneyAutotask.logProv.LogMessage("desperation set to: " + this.btcBuyFrequencyDesperation + "/" + this.BTC_BUY_FREQUENCY_DESPERATION_THRESHOLD);      
                    }
                }
            }
            
            //list of orders probably closed
            if(ordersNotOpen.size() > 0)
            {
                CryptomoneyAutotask.logProv.LogMessage("checking open orders to mark as complete...");
                for(Order missingOrder : ordersNotOpen)
                {
                    lsoc.library.utilities.Sleep.Sleep(100); //slow down so we're not querying too fast. this limits it to 10x/second
                    //this sleep might also give time for any cancel requests to finish

                    //Order actualOrder = Cbpdca.app.orderService().getOrder(missingOrder.getId());
                    //if(actualOrder == null)
                    //{
                    CryptomoneyAutotask.logProv.LogMessage("order was not found: " + missingOrder.getId()); //maybe check fills instead

                    boolean orderFound = false;
                    Fill fill = null;

                    int resultLimit = 20;
                    List<Fill> fills = CryptomoneyAutotask.app.orderService().getFillByOrderId(missingOrder.getId(), resultLimit);

                    if(fills.size() > 0)
                    {
                        for(Fill fillofList : fills)
                        {
                            if(missingOrder.getId().equals(fillofList.getOrder_id()))
                            {
                                fill = fillofList;
                                break;
                            }
                        }

                        if(fill != null)
                        {
                            String logString = "fill found: " + fill.getOrder_id() + " " + fill.getSize() + " " + fill.getLiquidity() + " " + fill.getProduct_id() + " " + fill.getSide() + " " + fill.getSettled();
                            CryptomoneyAutotask.logMultiplexer.LogMessage(logString); 
                            orderFound = true;
                            btcBuyFrequencyDesperation=0; //stub
                        }
                    }

                    if(!orderFound)
                    {
                        //we could compare ordersToCancel here
                        CryptomoneyAutotask.logProv.LogMessage("fill NOT FOUND: " + missingOrder.getId() + " assuming order was cancelled"); 
                        this.orders.remove(missingOrder.getId());
                        BigDecimal missingOrderPrice = BigDecimal.valueOf(Double.valueOf(missingOrder.getPrice()));
                        BigDecimal missingOrderCoinAmount = BigDecimal.valueOf(Double.valueOf(missingOrder.getSize()));
                        BigDecimal fiatEquivalentValueNotPurchased = missingOrderCoinAmount.multiply(missingOrderPrice);
                        this.getAllowanceCoinFiat(AllowanceType.Buy, _coinCurrencyType, _fiatCurrencyType).addToAllowance(fiatEquivalentValueNotPurchased);
                        madeChanges = true;
                        lsoc.library.utilities.Sleep.Sleep(200);
                    }
                    else
                    {                  
                        if(fill.getSettled())
                        {
                            //double filledSize = Double.valueOf(actualOrder.getFilled_size());
                            //double originalSize = Double.valueOf(actualOrder.getSize());
                            //double missedSize = originalSize - filledSize;


                            //TODO:ASSUMING COMPLETELY FILLED??
                            double missedSize = Double.parseDouble(missingOrder.getSize()) - fill.getSize().doubleValue();

                            if(missedSize > 0)
                            {
                                //add it back to allowance
                                this.getAllowanceCoinFiat(AllowanceType.Buy, _coinCurrencyType, _fiatCurrencyType).addToAllowance(BigDecimal.valueOf(missedSize));
                                madeChanges = true;
                            }

                            this.orders.remove(missingOrder.getId()); //remove order
                            CryptomoneyAutotask.logMultiplexer.LogMessage("ORDER DONE: " + missingOrder.getId() + " " + missingOrder.toString() + " filled: " + fill.getSize().doubleValue()+ "/" + missingOrder.getSize());
                            btcBuyFrequencyDesperation=0; //TODO: specify
                        }
                    }
                }
            }
        }
        else
        {
            CryptomoneyAutotask.logMultiplexer.LogException(new Exception("not implemented ProcessBuyOrders " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        return madeChanges;
    }
    
    public Order buyCoinImmediate(BigDecimal coinAmountToPurchase, CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType)
    {
        if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();
            
            
            
            CryptomoneyAutotask.logProv.LogMessage("do buy quantity: " + coinAmountToPurchase);
            
            //double purchasePrice = 3100.01; //TODO: IMPORTANT!: match one of the existing buy prices and use that.
            BigDecimal purchasePrice = SharedFunctions.GetBestCoinSellPrice(_coinCurrencyType, _fiatCurrencyType); //SELL price
            
            //TODO: make sure price isn't rediculous - like way above 24 hour avg
            
            coinAmountToPurchase = coinAmountToPurchase.setScale(8, RoundingMode.HALF_EVEN);
            
            purchasePrice = purchasePrice.setScale(2, RoundingMode.HALF_EVEN);
            
            Boolean post_only = false;
            String clientOid = UUID.randomUUID().toString();
            String type = "limit";
            String side = "buy";
            String product_id = TradingPair.getTradingPair(_coinCurrencyType, _fiatCurrencyType);//"BTC-USD";
            String stp = "cb"; //cancel both
            String funds = "";
                    
            CryptomoneyAutotask.logProv.LogMessage("coin size rounded to: " + coinAmountToPurchase);
            CryptomoneyAutotask.logProv.LogMessage("price rounded to: " + purchasePrice);
            
            NewOrderSingle newOrd = new NewLimitOrderSingle(coinAmountToPurchase, purchasePrice, post_only, clientOid, type, side, product_id, stp, funds);
            
            Order order = CryptomoneyAutotask.app.orderService().createOrder(newOrd); //API CALL
            this.orders.put(order.getId(), order);
            CryptomoneyAutotask.logProv.LogMessage("order placed, tracking client_oid: " +  order.getId() + " " + order.toString());
            //Cbpdca.logProv.LogMessage("order details: " + order.toString());
                
            //TODO: also need to verify after it gets filled to decrement allowance
            
            //wait a short while then check order status again
            lsoc.library.utilities.Sleep.Sleep(200);
            ProcessBuyOrders(_coinCurrencyType, _fiatCurrencyType, false);
            
            return order;
        }
        else
        {
            CryptomoneyAutotask.logMultiplexer.LogException(new Exception("not implemented buy coin " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        CryptomoneyAutotask.logMultiplexer.LogException(new Exception("unexpected result in buyCoinPostOnly"));
        return null;
    }
    
    /**
     * CALLS API
     */
    public Order buyCoinPostOnly(BigDecimal coinAmountToPurchase, CoinCurrencyType _coinCurrencyType, FiatCurrencyType _fiatCurrencyType)
    {
        if(this.exchangeType == ExchangeType.CoinbasePro)
        {
            //OrderService orderService = new com.coinbase.exchange.api.orders.OrderService();
            
            
            
            CryptomoneyAutotask.logProv.LogMessage("do buy quantity: " + coinAmountToPurchase);
            
            //double purchasePrice = 3100.01; //TODO: IMPORTANT!: match one of the existing buy prices and use that.
            BigDecimal purchasePrice = SharedFunctions.GetBestCoinBuyPrice(_coinCurrencyType, _fiatCurrencyType);
            
            //TODO: make sure price isn't rediculous - like way above 24 hour avg
            
            coinAmountToPurchase = coinAmountToPurchase.setScale(8, RoundingMode.HALF_EVEN);
            
            purchasePrice = purchasePrice.setScale(2, RoundingMode.HALF_EVEN);
            
            Boolean post_only = true; //note: this makes it so it won't execute the order immediately (might even cancel it), instead go to order book.  No fees for this!
            String clientOid = UUID.randomUUID().toString();
            String type = "limit";
            String side = "buy";
            String product_id = TradingPair.getTradingPair(_coinCurrencyType, _fiatCurrencyType);//"BTC-USD";
            String stp = "cb"; //cancel both
            String funds = "";
                    
            CryptomoneyAutotask.logProv.LogMessage("coin size rounded to: " + coinAmountToPurchase);
            CryptomoneyAutotask.logProv.LogMessage("price rounded to: " + purchasePrice);
            
            NewOrderSingle newOrd = new NewLimitOrderSingle(coinAmountToPurchase, purchasePrice, post_only, clientOid, type, side, product_id, stp, funds);
            
            if(newOrd == null)
            {
                CryptomoneyAutotask.logMultiplexer.LogException(new Exception("NewOrderSingle null, shouldn't be!"));
                return null;
            }
            
            Order order = CryptomoneyAutotask.app.orderService().createOrder(newOrd); //API CALL
            
            if(order == null)
            {
                String logInfo = "order respons is null, order probably failed";
                CryptomoneyAutotask.logMultiplexer.LogMessage(logInfo);
                return null;
            }
            
            this.orders.put(order.getId(), order);
            CryptomoneyAutotask.logProv.LogMessage("order placed, tracking client_oid: " +  order.getId() + " " + order.toString());
            //Cbpdca.logProv.LogMessage("order details: " + order.toString());
                
            //TODO: also need to verify after it gets filled to decrement allowance
            
            //wait a short while then check order status again
            lsoc.library.utilities.Sleep.Sleep(200);
            ProcessBuyOrders(_coinCurrencyType, _fiatCurrencyType, false);
            
            return order;
        }
        else
        {
            CryptomoneyAutotask.logMultiplexer.LogException(new Exception("not implemented buyCoinPostOnly " + this.exchangeType.toString()));
            System.exit(1);
        }
        
        CryptomoneyAutotask.logMultiplexer.LogException(new Exception("unexpected result in buyCoinPostOnly"));
        return null;
    }




}
