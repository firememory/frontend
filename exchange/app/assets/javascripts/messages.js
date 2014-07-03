var Messages = {
    buy: 'Buy',
    sell: 'Sell',
    buyShort: 'Buy',
    sellShort: 'Sell',
    unknown: 'Unknown',
    loading: 'Loading...'
};

Messages.orderStatus = {
    pending: 'Open',
    open: 'Partial',
    finished: 'Filled',
    cancelled: 'Cancelled',
    cancelling: 'Cancelling'
};

Messages.transfer = {
    status: ['Pending', 'Accepted', 'Confirming', 'Confirmed', 'Succeeded', 'Failed', 'Reorging', 'Reorg Succeeded', 'Cancelled', 'Rejected'],
    operation: ['Deposit', 'Withdrawal' , 'User to Hot', 'Hot to cold', 'Cold to hot', 'Unknown'],
    deposit: 'Deposit',
    withdrawal: 'Withdrawal',
    depositSuccess: 'Deposit Succeed: ',
    withdrawalSuccess: 'Withdrawal Succeed: ',
    cny: 'CNY',
    btc: 'BTC',
    messages: {
        invalidAmount: 'Invalid Amount',
        invalidAddress: 'Invalid Address',
        invalidSmsCode: 'Invalid SMS Code',
        ok: 'Request Submitted',
        error: 'Request failed'
    }
};

Messages.timeDemension = {
    w1 : '1w',
    d3: '3d',
    d1: '1d',
    h12: '12h',
    h6: '6h',
    h4: '4h',
    h2: '2h',
    h1: '1h',
    m30: '30m',
    m15: '15m',
    m5: '5m',
    m3:'3m',
    m1: '1m'
};

Messages.asset = {
    assetComposition: 'Composition',
    assetTrend: 'Trend of Assets'
};

Messages.trade = {
    lowerZero: 'Invalid quantity input - it must be greater than 0',
    noEnough: 'Insufficient balance - you have not enough to trade.',
    inputAmount: 'Invalid quantity input - it is required and must be greater than 0.',
    inputPrice: 'Invalid price input - it is required and must be greater than 0.',
    inputTotal: 'Invalid total input - it must be greater than 0. You can achieve this by increasing the price or quantity input.',
    submit: 'Order submitting',
    submitted: 'Order submitted',
    error: 'Fail to submit order'
};

Messages.account = {
    registerSucceeded: 'register succeeded',
    updateAccountProfileSucceeded: 'save profile succeeded',
    getVerifyCodeButtonText: 'Send sms code',
    getVerifyCodeButtonTextPrefix: 'Resend after ',
    getVerifyCodeButtonTextTail: ' seconds',
    getGoogleAuthCodeButtonText: 'Get authenticator code'
};

Messages.connectivity = {
    status: {normal: 'Normal', delayed: 'Delayed', blocked: 'Blocked'}
};

// defines error messages for errorCode in data.thrift
// See enum ErrorCode in data.thrift
// key = m + errorCode
Messages.ErrorMessages = {
    m500:  'Request timeout, please retry later.',
    m1001: 'Hmmm..., this email has been registered already.',
    m1002: 'missing information',
    m1003: 'Hmmm..., there is no such user in our system :(',
    m1004: 'Your password is incorrect.',
    m1005: 'token not match',
    m1006: 'This account has not been verified yet, check your email to verify your registration first.',

    m2001: 'Price is out of range',
    m2002: 'Insufficient fund',
    m2003: 'Invalid amount',
    m2009: 'Cryptocurrency Gateway failed',
    m2010: 'insufficient fund in hot wallets',
    m2011: 'insufficient fund in cold wallets',
    m2012: 'Invalid user',

    m9002: 'captcha text not match',
    m9003: 'Your provided email is invalid.',
    m9004: 'Your password is not valid.',
    m9005: 'SMS verification code is incorrect',
    m9006: 'Sorry, but you have to verify your mobile phone number first.',
    m9007: 'Sorry, but you send sms too freguency, please wait for a minute.',
    m9008: 'Sorry, your email should be same with the one in invite code page.',
    m9009: 'Sorry, your mobile phone is invalid.'
};

Messages.getMessage = function(code, msg) {
    var key = "m" + code;
    if (key in Messages.ErrorMessages) {
        return Messages.ErrorMessages[key];
    } else {
        if (msg !== undefined && msg.trim().length > 0)
            return msg;
        else
            return "internal error. errorCode=" + code;
    }
};
