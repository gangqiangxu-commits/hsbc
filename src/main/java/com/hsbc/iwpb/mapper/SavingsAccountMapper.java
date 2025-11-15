package com.hsbc.iwpb.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.hsbc.iwpb.entity.SavingsAccount;

@Mapper
public interface SavingsAccountMapper {

    @Insert("INSERT INTO savings_account(name, personal_id, balance, created_at, last_updated) VALUES(#{name}, #{personalId}, #{balance}, #{createdAt}, #{lastUpdated})")
    @Options(useGeneratedKeys = true, keyProperty = "accountNumber", keyColumn = "account_number")
    int insert(SavingsAccount account);

    @Select("SELECT account_number AS accountNumber, name, personal_id AS personalId, balance, created_at AS createdAt, last_updated AS lastUpdated FROM savings_account WHERE account_number = #{accountNumber}")
    SavingsAccount findByAccountNumber(long accountNumber);

    @Select("SELECT account_number AS accountNumber, name, personal_id AS personalId, balance, created_at AS createdAt, last_updated AS lastUpdated FROM savings_account")
    java.util.List<SavingsAccount> listAll();

    @Update("UPDATE savings_account SET name = #{name}, personal_id = #{personalId}, balance = #{balance}, last_updated = #{lastUpdated} WHERE account_number = #{accountNumber}")
    int update(SavingsAccount account);
}
