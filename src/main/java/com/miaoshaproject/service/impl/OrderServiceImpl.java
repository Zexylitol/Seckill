package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderDOMapper;
import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dao.StockLogDOMapper;
import com.miaoshaproject.dataobject.OrderDO;
import com.miaoshaproject.dataobject.SequenceDO;
import com.miaoshaproject.dataobject.StockLogDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @author yzze
 * @create 2021-06-14 16:17
 */
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional       // 保证创建订单是在同一个事务当中
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException {
        //1. 校验下单状态，下单的商品是否存在，下单的用户是否合法，购买数量是否正确
        //ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if(Objects.isNull(itemModel)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }
//        //UserModel userModel=userService.getUserById(userId);
//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if(Objects.isNull(userModel)) {
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
//        }
        if(amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }

        //校验活动信息
//        if(!Objects.isNull(promoId)) {
//            //(1)校验对应活动是否存在此适用商品
//            if(promoId.intValue() != itemModel.getPromoModel().getId()) {
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//                //校验活动是否正在进行中
//            } else if(itemModel.getPromoModel().getStatus() != 2) {
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
//            }
//        }

        //2. 落单减库存，支付减库存
        // 采用落单减库存
        boolean result = itemService.decreaseStock(itemId,amount);
        if(!result) {
            throw new BusinessException((EmBusinessError.STOCK_NOT_ENOUGH));
        }

        //3. 订单入库
        OrderModel orderModel=new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setPromoId(promoId);
        orderModel.setAmount(amount);
        if(!Objects.isNull(promoId)){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());  //取活动价格
        }else {
            orderModel.setItemPrice(itemModel.getPrice());
        }

        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));  //订单总金额


        //生成交易流水号/订单号，也就是对应订单表order_info中主键id的值，该值不能自增，需此生成
        orderModel.setId(generatorOrderNo());
        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //加上商品的销量
        itemService.increaseSales(itemId, amount);

        // 设置库存流水状态为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (Objects.isNull(stockLogDO)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        // 消息发送成功，若commit失败还是会导致库存被白白扣掉
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//            // 此方法会在最近的一个@Transactional注解成功commit之后执行
//            @Override
//            public void afterCommit() {
//                // 事务提交之后，异步更新库存
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
                    // 消息不能发送失败
////                if (!mqResult) {
////                    itemService.increaseStock(itemId, amount);
////                    // 异步更新库存失败，事务回滚
////                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });

        //4. 返回前端
        return orderModel;
    }

    private OrderDO convertFromOrderModel(OrderModel orderModel) {
        if(Objects.isNull(orderModel)){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);  //把模型层modle的值全部传递给持久化数据层orderDO
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice((orderModel.getOrderPrice().doubleValue()));
        return orderDO;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generatorOrderNo() {
        //订单号有 16 位
        StringBuilder stringBuilder=new StringBuilder();
        //前 8 位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-","");//将 - 替换成 "";
        stringBuilder.append(nowDate);

        // 中间 6 位位自增序列
        // 获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());

        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);

        String sequenceStr = String.valueOf(sequence);
        for(int i = 0; i < 6 - sequenceStr.length(); i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后 2 位位分库分表位,此处先固定不进行具体考虑
        stringBuilder.append("00");

        return stringBuilder.toString();
    }
}
