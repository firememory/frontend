var Messages = {
    buy: '买入',
    sell: '卖出',
    buyShort: '买',
    sellShort: '卖',
    unknown: '未知',
    loading: '加载中...'
};

Messages.orderStatus = {
    pending: '挂单中',
    open: '部分成交',
    finished: '全部成交',
    cancelled: '已撤单',
    cancelling: '正在撤单'
};

Messages.transfer = {
  status: ['等待处理', '处理中', '待确认', '已确认', '成功', '失败', '待确认'],
  operation: ['充值', '提现' , '用户转热钱包', '热钱包转冷钱包', '冷钱包转热钱包', '未知'],
  deposit: '充值',
  withdrawal: '提现',
  depositSuccess: '充值成功: ',
  withdrawalSuccess: '提现成功: ',
  cny: '人民币',
  btc: '比特币'
};

Messages.timeDemension = {
    w1 : '1周',
    d3: '3天',
    d1: '1天',
    h12: '12小时',
    h6: '6小时',
    h4: '4小时',
    h2: '2小时',
    h1: '1小时',
    m30: '30分钟',
    m15: '15分钟',
    m5: '5分钟',
    m3:'3分钟',
    m1: '1分钟'
};

Messages.asset = {
   assetComposition: '资产结构',
   assetTrend: '资产趋势'
};

Messages.trade = {
   lowerZero: '数量需要大于0',
   noEnough: '账户余额不足',
   inputAmount: '请输入数据',
   inputPrice: '请输入价格',
   inputTotal: '请输入金额',
   submit: '正在提交订单'
};

Messages.account = {
  registerSucceeded: '注册成功',
  updateAccountProfileSucceeded: '更新用户资料成功',
  getVerifyCodeButtonText: '获取短信验证码'
};

Messages.connectivity = {
  status: {normal: '正常', delayed: '延迟', blocked: '无法连通'}
};

Messages.ErrorMessages = {
    m1001: '额..., 此邮箱已被注册.',
    m1002: '缺少信息',
    m1003: '额..., 系统中查无此用户',
    m1004: '密码错误.',
    m1005: '验证失败',
    m1006: '此账号尚未激活，请先检查你的注册邮箱激活账号.',

    m9002: '验证码不匹配',
    m9003: 'email 格式不正确.',
    m9004: '密码格式不正确.',
    m9005: '短信校验码不匹配',
    m9006: '对不起，请先在账号－设置中绑定手机号.',
    m9008: '对不起，注册邮箱与邀请页面填写的邮箱不一致.'
};

Messages.getMessage = function(code, msg) {
    var key = "m" + code;
    if (key in Messages.ErrorMessages) {
        return Messages.ErrorMessages[key];
    } else {
        if (msg !== undefined && msg.trim().length > 0)
            return msg;
        else
            return " 服务端错误. 错误码=" + code;
    }
};
