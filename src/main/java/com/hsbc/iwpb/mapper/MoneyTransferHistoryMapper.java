package com.hsbc.iwpb.mapper;

import com.hsbc.iwpb.entity.MoneyTransferHistory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MoneyTransferHistoryMapper {
    @Insert("INSERT INTO money_transfer_history (transaction_id, source_account_number, destination_account_number, amount, created_at) VALUES (#{transactionId}, #{sourceAccountNumber}, #{destinationAccountNumber}, #{amount}, #{createdAt})")
    int insert(MoneyTransferHistory history);

    @Select("SELECT * FROM money_transfer_history WHERE transaction_id = #{transactionId}")
    @Results({
        @Result(property = "createdAt", column = "created_at")
    })
    MoneyTransferHistory findByTransactionId(@Param("transactionId") long transactionId);

    @Select("SELECT * FROM money_transfer_history WHERE source_account_number = #{accountNumber} ORDER BY created_at DESC")
    @Results({
        @Result(property = "createdAt", column = "created_at")
    })
    List<MoneyTransferHistory> findBySourceAccountNumber(@Param("accountNumber") long accountNumber);

    @Select("SELECT * FROM money_transfer_history WHERE destination_account_number = #{accountNumber} ORDER BY created_at DESC")
    @Results({
        @Result(property = "createdAt", column = "created_at")
    })
    List<MoneyTransferHistory> findByDestinationAccountNumber(@Param("accountNumber") long accountNumber);

    @Select("SELECT * FROM money_transfer_history WHERE source_account_number = #{sourceAccountNumber} AND destination_account_number = #{destinationAccountNumber} ORDER BY created_at DESC")
    @Results({
        @Result(property = "createdAt", column = "created_at")
    })
    List<MoneyTransferHistory> findBySourceAndDestinationAccountNumber(@Param("sourceAccountNumber") long sourceAccountNumber, @Param("destinationAccountNumber") long destinationAccountNumber);

    @Select("SELECT * FROM money_transfer_history ORDER BY created_at DESC")
    @Results({
        @Result(property = "createdAt", column = "created_at")
    })
    List<MoneyTransferHistory> listAll();
}