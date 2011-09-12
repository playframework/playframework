package play.api.db

import play.api._
import play.core._

import java.sql._
import javax.sql._

import com.jolbox.bonecp._

object DB extends Plugin {
    
    private def createDataSource(conf:Configuration) = {
        val datasource = new BoneCPDataSource
        
        // Try to load the driver
        conf.getString("driver").map { driver =>
            try { 
                Class.forName(driver) 
            } catch {
                case e => throw conf.reportError("driver", "Driver not found: " + driver, Some(e))
            }
        }
        
        // url is required
        conf.getString("url").map(datasource.setJdbcUrl(_)).orElse {
            throw conf.globalError("Missing url configuration for database " + conf.root)
        }
        
        conf.getString("user").map(datasource.setUsername(_))
        conf.getString("pass").map(datasource.setPassword(_))
        
        datasource -> conf.full("url")
    }
    
    private lazy val datasources:Map[String,(BoneCPDataSource,String)] = {
        Play.configuration.getSub("db").map { dbConf =>
            dbConf.subKeys.map { db =>
                db -> createDataSource(dbConf.getSub(db).get)
            }.toMap
        }.getOrElse(Map.empty)
    }
    
    // -- API
    
    def getConnection(name:String = "default"):Connection = datasources.get(name).get._1.getConnection
    
    // -- Plugin
    
    override def onStart {
        datasources.map { 
            case (name, (ds, config)) => {
                try {
                    ds.getConnection.close()
                    println("database:" + name + " connected at " + ds.getJdbcUrl)
                } catch {
                    case e => throw Play.configuration.reportError(config, "Cannot connect to database at " + ds.getJdbcUrl, Some(e.getCause))
                }
            }
        }
    }
    
    override def onStop {
        datasources.values.foreach {
            case (ds,_) => try{ ds.close() } catch { case _ => }
        }
    }
    
}