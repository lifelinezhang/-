## SOAP协议玩法

#### 一、整体思路

1、在java中调用soap协议接口，在拿到地址之后，可以先在地址后面加上?wsdl，然后在浏览器打开，另存为webservice.xml文件。之所以要用xml文件而不是直接用地址是因为如果直接用地址的话，后面会有坑。

2、使用工具cxf来生成客户端代码，cxf只需要下载解压，然后添加环境变量即可。为什么要使用cxf呢？因为如果使用axis2的话，生成完毕之后需要另外导入其他jar包，是个大坑；如果使用自带的wsimport的话，会有一些问题解决不了，也是个大坑，所以要使用cxf的wsdl2java命令。

#### 二、踩过的坑

1、属性 "xxxx" 已定义

2、具有相同名称 "xxxxx" 的类/接口已在使用（wsimport无法解决，cxf可以加参数）

3、不允许 'file' 访问或者http请求之类的

4、undefinedelement declaration 's:schema'（用<s:any minOccurs="2" maxOccurs="2"/>替代<s:element ref="s:schema"/><s:any />   ）

5、undefined simple or complex type 'soap-enc:Array'  则需要在生成的文件中找到 
<import namespace="http://schemas.xmlsoap.org/soap/encoding/" />， 
在浏览器中打开http://schemas.xmlsoap.org/soap/encoding/， 
保存文件schemas.xmlsoap.org.xml， 
然后修改成<import namespace="http://schemas.xmlsoap.org/soap/encoding/" schemaLocation="schemas.xmlsoap.org.xml"/>， 

6、axis2生成的话会导入不知道多少jar包，是个大坑

7、WSDLToJava Error: file:/F:/service5.rspread.net.xml [627,19]: 属性 "Any" 已定义。请使用 &lt;jaxb:property> 解决此冲突。
添加配置文件xsd.xjb
```
<?xml version="1.0" encoding="UTF-8"?>
<bindings xmlns="http://java.sun.com/xml/ns/jaxb"
          xmlns:xsd="http://www.w3.org/2001/XMLSchema"
          xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc"
          version="2.0">
 
  <globalBindings>
    <xjc:simple />
  </globalBindings>
 
  <bindings scd="~xsd:complexType">
    <class name="ComplexTypeType"/>
  </bindings>
 
  <bindings scd="~xsd:simpleType">
    <class name="SimpleTypeType"/>
  </bindings>
 
  <bindings scd="~xsd:group">
    <class name="GroupType"/>
  </bindings>
 
  <bindings scd="~xsd:attributeGroup">
    <class name="AttributeGroupType"/>
  </bindings>
 
  <bindings scd="~xsd:element">
    <class name="ElementType"/>
  </bindings>
 
  <bindings scd="~xsd:attribute">
    <class name="attributeType"/>
  </bindings>
</bindings>
```
使用命令：`wsimport(或者wsdl2java) -b http://www.w3.org/2001/XMLSchema.xsd -b xsd.xjb SecureConversation.wsdl`
#### 三、生成客户端的命令
wsdl2java -encoding utf-8 -b http://www.w3.org/2001/XMLSchema.xsd -b F:\xsd.xjb  -d F:\test F:\reasonablespread.xml
