# EntityToDBService 基础组件说明：根据项目中的实体生成数据库和对应数据表

# 基础组件实现背景：
根据数据库及其表结构来生成对应的数据实体，这个过程在网络上已经被实现了，且存在一定的框架实现基础
但是，根据项目中的实体生成数据库和对应数据表在网络上并不多，且网络上存在的生成实现并不灵活，也不能进行私人定制
于是，本基础组件的设计目标就是为了解决这一问题

# 基础组件实现目标：
EntityToDBService 服务采用Java语言实现，基于原生JDBC实现项目实体到项目数据库及其数据表的生成功能

# 使用过程：
1. 采用本服务默认生成规则实现生成：

直接调用 EntityToDBService.entityToDB 方法生成，看到控制台打印数据表生成成功即可。
该方法的第一个参数是需要生成的数据库名称，第二个参数是需要生成的实体所在的包路径。

2. 定制生成规则实现生成：

根据自己的实际需求，修改entityToDB方法中对应的sql规则即可。

# EntityToDBService Basic component description: Generate database and corresponding data table according to the entities in the project

# Basic component implementation background:
According to the database and its table structure to generate the corresponding data entities, this process has been implemented on the network, and there is a certain framework implementation basis
However, there are not many databases and corresponding data tables generated from entities in the project on the Internet, and the generation implementations existing on the Internet are not flexible and cannot be customized.
Therefore, the design goal of this basic component is to solve this problem

# Basic components achieve goals:
The EntityToDBService service is implemented in Java language, based on native JDBC to realize the generation function of project entity to project database and its data table

# Use process:
1. Use the default generation rules of this service to generate:

Directly call the EntityToDBService.entityToDB method to generate, and see that the console print data table is generated successfully.
The first parameter of this method is the name of the database to be generated, and the second parameter is the package path where the entity to be generated is located.

2. Customize the generation rules to achieve generation:

According to your actual needs, you can modify the corresponding sql rules in the entityToDB method.
