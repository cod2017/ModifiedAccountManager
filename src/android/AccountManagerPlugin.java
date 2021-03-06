// Copyright (C) 2013 Polychrom Pty Ltd
//
// This program is licensed under the 3-clause "Modified" BSD license,
// see LICENSE file for full definition.

package com.polychrom.cordova;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.string;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import android.content.ContentResolver;

/**! Android AccountManager plugin for Cordova
 *
 * @author Mitchell Wheeler
 *
 *	Implements a basic Android AccountManager plugin for Cordova with support for common account management routines.
 *
 *	Features not currently supported are:
 *  *  Account features
 *  *  Automatic Authentication via AccountManagers (only explicit accounts and auth tokens are supported currently)
 */
public class AccountManagerPlugin extends CordovaPlugin
{
	AccountManager manager = null;
  String authtoken=null;
	// Naive int to account mapping so our JS can simply reference native objects
	Integer accumulator = 0;
  Account[] availableAccounts;
	HashMap<Integer, Account> accounts = new HashMap<Integer, Account>();
	Account tempAccount;
  public static final String AUTH_TOKEN_TYPE="Bearer";
  public static final String INFINCE_USER = "INF_FIN_USER";
    public static final String INF_FIN_BASE_URL = "INF_FIN_BASE_URL";

  CallbackContext callbackContext;
	private Integer indexForAccount(Account account)
	{
		for(Entry<Integer, Account> e: accounts.entrySet())
		{
			if(e.getValue().equals(account))
			{
				return e.getKey();
			}
		}

		accounts.put(accumulator, account);
		return accumulator++;
	}
  private String getExistingAccountAuthToken(Account account,  final String authTokenType,final String name) {

    final AccountManagerFuture<Bundle> future = manager.getAuthToken(account, authTokenType, false, null, null);
	final String userdata= manager.getUserData(account, INFINCE_USER);
		final String baseUrl= manager.getUserData(account, INF_FIN_BASE_URL);

     new Thread(new Runnable() {
      @Override
      public void run() {
        try {
            Bundle bnd = future.getResult();
            authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
          JSONArray result = new JSONArray();
          JSONObject account_object = new JSONObject();
          account_object.put("token", authtoken);
          account_object.put("name", name);
					account_object.put("type", authTokenType);
										account_object.put("userdata", userdata);
																				account_object.put("baseUrl", baseUrl);


          result.put(account_object);



        callbackContext.success(result);
        } catch (Exception e) {
          e.printStackTrace();
		  
          // showMessage(e.getMessage());
        }
      }
    }).start();
return authtoken;


  }
  private boolean isAutoSync() {
return ContentResolver.getMasterSyncAutomatically();
//ContentResolver.setSyncAutomatically(account, authority, true/false);

}
private void synAccount(){
ContentResolver.setMasterSyncAutomatically(true);

}

  @Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException
	{
    this.callbackContext=callbackContext;
		if(manager == null)
		{
			manager = AccountManager.get(cordova.getActivity());
		}

		try
		{
			if("getAccountsByType".equals(action))
			{
				 availableAccounts = manager.getAccountsByType(args.isNull(0)? null : args.getString(0));

				JSONArray result = new JSONArray();
				if (availableAccounts.length==0) {
					  callbackContext.success("result");
				
				} else {
				   for (Account account: availableAccounts) {
				    Integer index = indexForAccount(account);
				    String accessToken = getExistingAccountAuthToken(account, AUTH_TOKEN_TYPE, account.name);

				    //
				    //          JSONObject account_object = new JSONObject();
				    //					account_object.put("_index", (int)index);
				    //					account_object.put("name", account.name);
				    //					account_object.put("type", account.type);
				    //          account_object.put("token", accessToken);
				    //
				    //          result.put(account_object);
				    //					this.tempAccount=account;
				  }

				}


		

				//callbackContext.success(result);
				return true;
			}
			//   else if (action.equals("checkAutoSync")) {

			//     // alert(args.getString(0), args.getString(1), args.getString(2), callbackContext);
			//     boolean temp = isAutoSync();
			//     final PluginResult result = new PluginResult(PluginResult.Status.OK, temp);
			//     callbackContext.sendPluginResult(result);

			//     return true;

			//   }

			else if("addAccountExplicitly".equals(action))
			{
				if(args.isNull(0) || args.getString(0).length() == 0)
				{
					callbackContext.error("accountType can not be null or empty");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("username can not be null or empty");
					return true;
				}
				else if(args.isNull(2) || args.getString(2).length() == 0)
				{
					callbackContext.error("password can not be null or empty");
					return true;
				}

				Account account = new Account(args.getString(1), args.getString(0));
				Integer index = indexForAccount(account);

				Bundle userdata = new Bundle();
				if(!args.isNull(3))
				{
					JSONObject userdata_json = args.getJSONObject(3);
					if(userdata_json != null)
					{
						Iterator<String> keys = userdata_json.keys();
						while(keys.hasNext())
						{
							String key = keys.next();
							userdata.putString(key, userdata_json.getString(key));
						}
					}
				}

				if(false == manager.addAccountExplicitly(account, args.getString(2), userdata))
				{
					callbackContext.error("Account with username already exists!");
					return true;
				}

				JSONObject result = new JSONObject();
				result.put("_index", (int)index);
				result.put("name", account.name);
				result.put("type", account.type);

				callbackContext.success(result);
				return true;
			}
			else if("updateCredentials".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				callbackContext.error("Not yet implemented");
				return true;
			}
			else if("clearPassword".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				manager.clearPassword(account);
				callbackContext.success();
				return true;
			}
			else if("removeAccount".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}

				int index = args.getInt(0);
				Account account = accounts.get(index);
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				// TODO: Add support for AccountManager (callback)
				AccountManagerFuture<Boolean> future = manager.removeAccount(account, null, null);
				try
				{
					if(future.getResult() == true)
					{
						accounts.remove(index);
						callbackContext.success();
					}
					else
					{
						callbackContext.error("Failed to remove account");
					}
				}
				catch (OperationCanceledException e)
				{
					callbackContext.error("Operation canceled: " + e.getLocalizedMessage());
				}
				catch (AuthenticatorException e)
				{
					callbackContext.error("Authenticator error: " + e.getLocalizedMessage());
				}
				catch (IOException e)
				{
					callbackContext.error("IO error: " + e.getLocalizedMessage());
				}

				return true;
			}
			else if("setAuthToken".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("authTokenType can not be null or empty");
					return true;
				}
				else if(args.isNull(2) || args.getString(2).length() == 0)
				{
					callbackContext.error("authToken can not be null or empty");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				manager.setAuthToken(account, args.getString(1), args.getString(2));
				callbackContext.success();
				return true;
			}
			else if("peekAuthToken".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("authTokenType can not be null or empty");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				JSONObject result = new JSONObject();
				result.put("value", manager.peekAuthToken(account, args.getString(1)));
				callbackContext.success(result);
				return true;
			}
			else if("getAuthToken".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("authTokenType can not be null or empty");
					return true;
				}
				else if(args.isNull(2))
				{
					callbackContext.error("notifyAuthFailure can not be null");
					return true;
				}

				// Account account = args.
    			Account account = this.tempAccount;
				Integer index = indexForAccount(account);

					JSONObject account_object = new JSONObject();
						account_object.put("_index", (int)index);
					account_object.put("name", account.name);
					account_object.put("type", account.type);
					// result.put(account_object);
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}
				else{
					// 				Bundle options = new Bundle();
					// 			AccountManagerFuture<Bundle> future = manager.getAuthToken(account, args.getString(1), false, null, null);
					// try {
					// JSONObject result = new JSONObject();
					// result.put("value", future.getResult().getString(AccountManager.KEY_AUTHTOKEN));
					// callbackContext.success(result);
					// return true;
					// } catch (OperationCanceledException e) {
					// callbackContext.error("Operation canceled: " + e.getLocalizedMessage());
					// return true;
					// } catch (AuthenticatorException e) {
					// callbackContext.error("Authenticator error: " + e.getLocalizedMessage());
					// return true;
					// } catch (IOException e) {
					// callbackContext.error("IO error: " + e.getLocalizedMessage());
					// return true;
					// }


					 final AccountManagerFuture < Bundle > future = manager.getAuthToken(account, args.getString(1), false, null, null);

					 // new asynTaskGetToken(future).execute();



					 new Thread(new Runnable() {
					   @Override
					   public void run() {
					     try {
					       Bundle bnd = future.getResult();

					       final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
					       JSONObject result = new JSONObject();
					       result.put("value", authtoken);
					       callbackContext.success(result);
					    //    return true;

					     } catch (Exception e) {
					       callbackContext.error("IO error: " + e.getLocalizedMessage());
					    //   return true;
					       // showMessage(e.getMessage());
					     }
					   }
					 }).start();



					 // 	callbackContext.error(account_object.toString());
					 // return true;
					 // 	callbackContext.error(account_object.toString());
					 // return true;
					 }


				// TODO: Options support (will be relevent when we support AccountManagers)

				// TODO: AccountManager support




			}
			else if("setPassword".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("password can not be null or empty");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				manager.setPassword(account, args.getString(1));
				callbackContext.success();
				return true;
			}
			else if("getPassword".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				JSONObject result = new JSONObject();
				result.put("value", manager.getPassword(account));
				callbackContext.success(result);
				return true;
			}
			else if("setUserData".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("key can not be null or empty");
					return true;
				}
				else if(args.isNull(2) || args.getString(2).length() == 0)
				{
					callbackContext.error("value can not be null or empty");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				manager.setUserData(account, args.getString(1), args.getString(2));
				callbackContext.success();
				return true;
			}
			else if("getUserData".equals(action))
			{
				if(args.isNull(0))
				{
					callbackContext.error("account can not be null");
					return true;
				}
				else if(args.isNull(1) || args.getString(1).length() == 0)
				{
					callbackContext.error("key can not be null or empty");
					return true;
				}

				Account account = accounts.get(args.getInt(0));
				if(account == null)
				{
					callbackContext.error("Invalid account");
					return true;
				}

				JSONObject result = new JSONObject();
				result.put("value", manager.getUserData(account, args.getString(1)));
				callbackContext.success(result);
				return true;
			}
		}
		catch(SecurityException e)
		{
			callbackContext.error("Access denied");
			return true;
		}

		return false;
	}
}
