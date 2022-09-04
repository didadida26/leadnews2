<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Hello World!</title>
</head>
<body>

<#-- list 数据的展示 -->
<b>展示list中的stu数据:</b>
<br>
<br>
<table>
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
    </tr>
    <#list stus as stu>
        <tr>
            <td>${stu_index + 1}</td>
            <td>${stu.name}</td>
            <td>${stu.age}</td>
            <td>${stu.money}</td>
        </tr>
    </#list>

</table>
<hr>

<#-- Map 数据的展示 -->
<b>map数据的展示：</b>
<br/><br/>
<a href="###">方式一：通过map['keyname'].property</a><br/>
输出stu1的学生信息：<br/>
姓名：${stuMap['stu1'].name} <br/>
年龄：${stuMap['stu1'].age} <br/>
<br/>
<a href="###">方式二：通过map.keyname.property</a><br/>
输出stu2的学生信息：<br/>
姓名：${stuMap.stu1.name} <br/>
年龄：${stuMap.stu1.age} <br/>

<br/>
<a href="###">遍历map中两个学生信息：</a><br/>
<table>
    <tr>
        <td>序号</td>
        <td>姓名</td>
        <td>年龄</td>
        <td>钱包</td>
    </tr>
    <#list stuMap?keys as key>

        <#if stuMap[key].name = '小红'>
            <tr style="color: #1ff000;">
                <td>${key_index + 1}</td>
                <td>${stuMap[key].name}</td>
                <td>${stuMap[key].age}</td>
                <td>${stuMap[key].money}</td>
            </tr>
        <#else >
            <tr>
                <td>${key_index + 1}</td>
                <td>${stuMap[key].name}</td>
                <td>${stuMap[key].age}</td>
                <td>${stuMap[key].money}</td>
            </tr>

        </#if>


    </#list>
    集合的大小：${stuMap?size}


</table>
<hr>
100+5 运算： ${100 + 5 }<br/>
100 - 5 * 5运算：${100 - 5 * 5}<br/>
5 / 2运算：${5 / 2}<br/>
12 % 10运算：${12 % 10}<br/>


<b>比较运算符</b>
<br/>
<br/>

<dl>
    <dt> =/== 和 != 比较：</dt>
    <dd>
        <#if "xiaoming" == "xiaoming">
            字符串的比较 "xiaoming" == "xiaoming"
        </#if>
    </dd>
    <dd>
        <#if 10 != 100>
            数值的比较 10 != 100
        </#if>
    </dd>
</dl>


<dl>
    <dt>其他比较</dt>
    <dd>
        <#if 10 gt 5 >
            形式一：使用特殊字符比较数值 10 gt 5
        </#if>
    </dd>
    <dd>
        <#-- 日期的比较需要通过?date将属性转为data类型才能进行比较 -->
        <#if date1?? || date2??> <#-- ?? 作用是防止date1 或者date2为空-->
            <#if (date1?date >= date2?date)>
                形式二：使用括号形式比较时间 date1?date >= date2?date
            </#if>
        </#if>

    </dd>
</dl>

<br/>
<hr>

<b>逻辑运算符</b>
<br/>
<br/>
<#if (10 lt 12 )&&( 10  gt  5 )  >
    (10 lt 12 )&&( 10  gt  5 )  显示为 true
</#if>
<br/>
<br/>
<#if !false>
    false 取反为true
</#if>
<hr>

显示年月日: ${today?date}
显示时分秒：${today?time}
显示日期+时间：${today?datetime}
自定义格式化： ${today?string("yyyy年MM月")}
<br/>
长数值类型：${point?c}

<br/>
将json字符串转成对象
<#assign text="{'bank':'工商银行','account':'10101920201920212'}" />
<#assign data=text?eval />
开户行：${data.bank} 账号：${data.account}

</body>
</html>