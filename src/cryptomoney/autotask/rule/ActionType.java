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
package cryptomoney.autotask.rule;

/**
 *
 * @author onyxcoyote <no-reply@onyxcoyote.com>
 */
public enum ActionType
{
    ALLOWANCE_BUY_COIN,
    ALLOWANCE_WITHDRAW_COIN_TO_COINBASE,
    ALLOWANCE_DEPOSIT_FIAT,
    
    ACTION_BUY_COIN_DCA_POSTONLY,
    ACTION_WITHDRAW_COIN_TO_COINBASE,
    ACTION_DEPOSIT_FIAT,
    
    ACTION_PROCESS_COIN_BUY_POST_ORDERS,
    
    ALARM_LOW_USD, //todo: implement
    ALARM_LOW_BTC, //todo: implement
    ALARM_PRINT_BALANCE
}
