# 1. 介绍
本项目是采用Vert.x开发的个人博客系统，分成两个模块，blog-core和blog-proxy，blog-core主要是博客系统常规功能的实现的，blog-proxy实现了文章阅读计数系统，能在高并发的情况下实现数据的异步落地，本项目前端页面地址为[链接](https://github.com/fanjianan916/blog-html/tree/master)
# 2. 部署
1. 首先需要将两个模块进行打称jar包
2. 打成jar包之后分别进行执行
3. ```plain
    java -jar ****.jar -cluster -conf config.json
    ```
4. config.json的格式如下
5. ```plain
    {
      "provider_class": "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider",
      "jdbcUrl": "jdbc:mysql://localhost:3306/beanan_blog?characterEncoding=utf8&useSSL=true",
      "driver_class": "com.mysql.cj.jdbc.Driver",
      "username": "****",
      "password": "****",
      "http_port": ****,
      "redis_host": "****",
      "redis_port": ****
    }
    ```

