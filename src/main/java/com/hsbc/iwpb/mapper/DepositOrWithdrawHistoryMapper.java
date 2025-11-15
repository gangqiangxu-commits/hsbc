package com.hsbc.iwpb.mapper;

import com.hsbc.iwpb.entity.DepositOrWithdrawHistory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DepositOrWithdrawHistoryMapper {
    @Insert("INSERT INTO deposit_withdraw_history (account_number, amount, created_at, transaction_id) VALUES (#{accountNumber}, #{amount}, #{createdAt}, #{transactionId})")
    @Options(useGeneratedKeys = true, keyProperty = "transactionId")
    int insert(DepositOrWithdrawHistory history);

    @Select("SELECT * FROM deposit_withdraw_history WHERE account_number = #{accountNumber} ORDER BY created_at DESC")
    List<DepositOrWithdrawHistory> findByAccountNumber(@Param("accountNumber") long accountNumber);

    @Select("SELECT * FROM deposit_withdraw_history WHERE transaction_id = #{transactionId}")
    DepositOrWithdrawHistory findByTransactionId(@Param("transactionId") long transactionId);

    @Select("SELECT * FROM deposit_withdraw_history ORDER BY created_at DESC")
    List<DepositOrWithdrawHistory> listAll();
}
