package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.quickbuy.entity.User;

@Mapper
public interface UserMapper {
    User selectById(@Param("id") Long id);
    User selectByUsername(@Param("username") String username);
    User selectByPhone(@Param("phone") String phone);
    User selectByEmail(@Param("email") String email);
    int insert(User user);
    int update(User user);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
} 