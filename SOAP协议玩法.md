## SOAP协议玩法

#### 一、整体思路

1、在java中调用soap协议接口，在拿到地址之后，可以先在地址后面加上?wsdl，然后在浏览器打开，另存为webservice.xml文件。之所以要用xml文件而不是直接用地址是因为如果直接用地址的话，后面会有坑。

2、使用工具cxf来生成客户端代码，cxf只需要下载解压，然后添加环境变量即可。为什么要使用cxf呢？因为如果使用axis2的话，生成完毕之后需要另外导入其他jar包，是个大坑；如果使用自带的wsimport的话，会有一些问题解决不了，也是个大坑，所以要使用cxf的wsdl2java命令。

#### 二、踩过的坑

1、属性 "xxxx" 已定义

2、具有相同名称 "xxxxx" 的类/接口已在使用（wsimport无法解决，cxf可以加参数）

3、不允许 'file' 访问或者http请求之类的

4、undefinedelement declaration 's:schema'（用<s:any minOccurs="2" maxOccurs="2"/>替代<s:element ref="s:schema"/><s:any />   ）

5、undefined simple or complex type 'soap-enc:Array'  （然后修改成<import namespace="http://schemas.xmlsoap.org/soap/encoding/" schemaLocation="soap-encoding.xsd"/>， ）

6、axis2生成的话会导入不知道多少jar包，是个大坑