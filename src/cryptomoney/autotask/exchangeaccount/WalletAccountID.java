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

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public class WalletAccountID
{
    private WalletAccountType walletAccountType;
    private WalletAccountCurrency walletAccountCurrency;
    private String account_Id; 
    
    public WalletAccountID(WalletAccountType _walletAccountType, WalletAccountCurrency _walletAccountCurrency, String _account_Id)
    {
        walletAccountType= _walletAccountType;
        walletAccountCurrency = _walletAccountCurrency;
        account_Id = _account_Id;
    }
    
    public void setAccount_Id(String _account_ID) throws Exception
    {
        if(this.account_Id == null)
            this.account_Id = _account_ID;
        else
            throw new Exception("can't set account ID already set");
    }
    
    public String getAccount_Id()
    {
        return this.account_Id;
    }

    /**
     * @return the walletAccountType
     */
    public WalletAccountType getWalletAccountType()
    {
        return walletAccountType;
    }

    /**
     * @return the walletAccountCurrency
     */
    public WalletAccountCurrency getWalletAccountCurrency()
    {
        return walletAccountCurrency;
    }
    
}
