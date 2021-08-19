const fs = require('fs')
const path = require('path')
const dotenv = require('dotenv')
const AssetsPlugin = require('assets-webpack-plugin')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin')

module.exports = (env, args) => {

  dotenv.config({ path: '.env.' + args.mode })
  // dotenv.parse(fs.readFileSync('.env.override'))

  const isProd = process.env.NODE_ENV === 'production'
  const siteDir = './sites'
  const destPath = path.resolve(__dirname, '../resources/dest')
  const assetsPath = path.resolve(__dirname, '../../java/site/core/src/main/resources')

  const entry = (function () {
    const entry = {}
    fs.readdirSync(path.resolve(__dirname, siteDir)).forEach(site => {
      if(!fs.statSync(path.resolve(__dirname, [siteDir, site].join('/'))).isDirectory()) return
      fs.readdirSync(path.resolve(__dirname, [siteDir, site].join('/'))).forEach(file => {
        if(!fs.statSync(path.resolve(__dirname, [siteDir, site, file].join('/'))).isFile()) return
        if (file.endsWith('.js')) entry[[site, file.substring(0, file.length - 3)].join('/')] = [siteDir, site, file].join('/')
      })
    })
    return entry
  })()

  console.log('*** config variables:')
  console.log('NODE_ENV:', process.env.NODE_ENV)
  console.log('destPath:', destPath)
  console.log('entry:', entry)

  return Object.assign({
    entry: entry,
    resolve: {
      alias: {
        '@': path.resolve('./')
      }
    },
    externals: {
      jQuery: 'jQuery'
    },
    module: {
      rules: [{
        test: /\.(png|jpg|jpeg|gif|eot|ttf|svg|woff|woff2)$/,
        use: ['url-loader']
      }, {
        test: /\.css$/i,
        use: [MiniCssExtractPlugin.loader, 'css-loader'],
      }, {
        test: /\.less$/,
        use: [MiniCssExtractPlugin.loader, 'css-loader', 'less-loader']
      }, {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env'],
            cacheDirectory: true
          }
        }
    }]
    },
    plugins: [
      new CleanWebpackPlugin(), // 清理目标目录
      new AssetsPlugin({ // 生成Hash对应关系文件
        path: assetsPath,
        filename: 'assets.json',
        update: false, // 非覆盖式更新
        prettyPrint: true,
        removeFullPathAutoPrefix: true
      }),
      new MiniCssExtractPlugin({ // 生成CSS文件
        filename: isProd ? '[name].[chunkhash].css' : '[name].css'
      })
    ]
  }, isProd ? {
    output: {
      path: destPath,
      filename: '[name].[chunkhash].js',
      chunkFilename: '[name].[chunkhash].min.js',
      hotUpdateMainFilename: '[hash].hot-update.json'
    },
    optimization: {
      minimize: true,
      minimizer: [
        new CssMinimizerPlugin(), // 压缩CSS文件
        new UglifyJsPlugin({ // 压缩JS文件
          cache: true,
          parallel: true
        })
      ]
    },
    bail: true // 在第一个错误出错时抛出，而不是无视错误
  } : {
    output: {
      path: destPath,
      filename: '[name].js',
      chunkFilename: '[name].min.js'
    },
    cache: true,
    watch: true,
    watchOptions: {
      aggregateTimeout: 300, // 将多个更改聚合到单个重构建，单位毫秒
      poll: 1000, // 间隔，单位毫秒
      ignored: '/node_modules/'
    }
  })

}
