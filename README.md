#### 1. 原始文档 https://github.com/aws-amplify/amplify-android
#### 2. 修改内容
- 修复一系列amplify崩溃
- 根据目前需求去掉本地数据库变更监听逻辑
- 去掉subscription及websocket更新逻辑（过于缓慢），仅保留appsync逻辑
- 修改串行appsync请求为并行，提高数据拉取时间

#### 3. 修改范围
所有修改module都需要打包aar并上传。目前修改的module：
- core
- aws-api
- aws-api-appsync
- aws-datastore
- aws-storage-s3

#### 3. 版本管理
在gradle.properties文件修改版本号，
版本号沿用amplify的本身版本，便于跟踪比较。仅在版本号后增加 '-atlasv*' 来进行标识，v1,v2,v3……等等，如：1.18.0-atlasv3
