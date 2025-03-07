CREATE TABLE t (
    id int NOT NULL AUTO_INCREMENT PRIMARY KEY comment '自增主键',
    dept int not null comment '部门id',
    age int not null comment '年龄',
    name varchar(30) comment '用户名称',
    create_time datetime not null comment '注册时间',
    last_login_time varchar(32) comment '最后登录时间'
) comment '测试表';

create index idx_dept on t(dept);
create index idx_create_time on t(create_time);
create index idx_last_login_time on t(last_login_time);


insert into t values(1,1, 25, 'user_1', '2018-01-01 00:00:00', '2018-03-01');

CREATE DEFINER=`root`@`%` PROCEDURE `fs_test`.`test_insert`()
BEGIN

DECLARE i INT DEFAULT 1;

set @i=1;

WHILE i<29
DO
insert into t(dept, age, name, create_time, last_login_time)
select left(rand()*1000,10000) as dept,
       FLOOR(20+RAND() *(150 - 20 + 1)) as age,
        concat('user_',@i:=@i+1),
        date_add(create_time,interval +@i*cast(rand()*100 as signed) SECOND),
        date_format(date_add(date_add(create_time,interval +@i*cast(rand()*100 as signed) SECOND), interval + cast(rand()*1000000 as signed) SECOND), '%Y-%m-%d')
from t;
SET i=i+1;
END WHILE ;
commit;

END

call test_insert();
