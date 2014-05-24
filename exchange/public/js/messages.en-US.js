var Messages = {
    buy: 'Buy',
    sell: 'Sell',
    buyShort: 'Buy',
    sellShort: 'Sell',
    unknown: 'Unknown'
};

Messages.orderStatus = {
    pending: 'Pending',
    open: 'Open',
    finished: 'Finished',
    cancelled: 'Cancelled'
};

Messages.transfer = {
    status: ['Pending', 'Accepted', 'Confirming', 'Confirmed', 'Succeeded', 'Failed', 'Confirming'],
    deposit: 'Deposit',
    withdrawal: 'Withdrawal',
    depositSuccess: 'Deposit Succeed: ',
    withdrawalSuccess: 'Withdrawal Succeed: ',
    cny: 'CNY',
    btc: 'BTC'
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
    assetComposition: 'Composition of Assets',
    assetTrend: 'Trend of Assets'
}

Messages.trade = {
    lowerZero: 'amount should bigger than zero',
    noEnough: 'account balance is not enough',
    inputAmount: 'please input amount',
    inputPrice: 'please input price',
    submit: 'submitting the order'
};

Messages.account = {
    registerSucceeded: 'register succeeded',
    updateAccountProfileSucceeded: 'save profile succeeded',
    getVerifyCodeButtonText: 'Get sms verify code'
};

Messages.connectivity = {
    status: {normal: 'Normal', delayed: 'Delayed', blocked: 'Blocked'}
};

Messages.ErrorMessages = {
    M1006: 'email not verified',
    M9002: 'captcha text not match'
};

Messages.getMessage = function(code) {
    var key = "M" + code;
    if (key in Messages.ErrorMessages) {
        return Messages.ErrorMessages[key];
    } else {
        return "internal error. errorCode=" + code;
    }
};
