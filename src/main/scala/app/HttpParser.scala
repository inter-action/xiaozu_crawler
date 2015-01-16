package app

import java.io._
import java.util
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.{XPathConstants, XPathFactory, XPath}

import com.mongodb.{DBObject, BasicDBObject, BasicDBObjectBuilder}
import org.cyberneko.html.parsers.DOMParser
import org.w3c.dom.{Element, Node, NodeList, Document}
import org.xml.sax.InputSource

import scala.collection.mutable.{ListBuffer, ArrayBuffer}

/**
 * Created by miaojing on 15-1-13.
 */
object HttpParser {
  private val parser = new DOMParser

  {
    /*The Xerces HTML DOM implementation does not support namespaces
    and cannot represent XHTML documents with namespace information.
    Therefore, in order to use the default HTML DOM implementation with NekoHTML's
    DOMParser to parse XHTML documents, you must turn off namespace processing.*/
    parser.setFeature("http://xml.org/sax/features/namespaces", false)

  }
  //如何正确结合NekoHTML和XPath XPath的Tag必须大写
  private val xpath: XPath = XPathFactory.newInstance().newXPath()


  def getDocument(ins: InputStream ): Document = {
    val inputStreamReader = new InputStreamReader(ins, "utf-8")
    val inputSource = new InputSource()
    inputSource.setCharacterStream(inputStreamReader);
    parser.parse(inputSource)
    parser.getDocument
  }

  //返回所有的导航链接
  def getCatLinks(ins: InputStream): List[Tuple2[String, String]] = {
    val path = "//DIV[contains(@class, 'caileinav')]/DIV[contains(@class, 'cailei')]//LI/A"
    val nodeList: NodeList = xpath.compile(path).evaluate(getDocument(ins), XPathConstants.NODESET).asInstanceOf[NodeList]
    val results: ListBuffer[Tuple2[String, String]] = ListBuffer()
    for (_i <- 0 until nodeList.getLength){
      val element = nodeList.item(_i).asInstanceOf[Element]
      val linkname = element.getTextContent.asInstanceOf[String]
      val href: String = element.getAttribute("href")

      if (href.isEmpty || href.contains("javascript") || href.contains("(")){
        //do nothing here
      }else{
        val _e:Tuple2[String, String] = (linkname, buildUrl(href))
        results += _e
      }
    }
    results.remove(results.length - 1)//remove last one
    results.toList
  }

  def buildUrl(url: String): String = {
    val root = "http://fuhua.xiaozufan.com"
    root + url
  }

  //荤素配的信息提取
  def parseCateA(ins: InputStream): java.util.List[DBObject] = {
    val document = getDocument(ins)

    //遍历每一家的菜品
    //选择最后一个class="bid-card "的div
    val cardsPath = "//DIV[contains(@class, 'bid-cards')]/DIV[@class='bid-card ']"
    val cardNodes: NodeList = xpath.compile(cardsPath).evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]


    val catContents: java.util.List[DBObject] = new util.ArrayList[DBObject]()

    println(s"商户节点: ${cardNodes.getLength} " )
    for (_i <- 0 until cardNodes.getLength) {
      var node = cardNodes.item(_i)
      var _nMerchant: Node = xpath.compile(".//A[contains(@class, 'shop_aw')][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      //获得子类下的商户名
      val _tMerchant = _nMerchant.getFirstChild().getTextContent()
      //商户ID
      val bid = xpath.compile(".//FORM/INPUT[@type='hidden'][@name='bid']/@value").evaluate(node, XPathConstants.STRING).asInstanceOf[String]
      println(s"\n+ sub cat merchant - merchant name: ${_tMerchant},  bid: $bid \n")

      //用提交按钮来判断是否商户是否停售
      val _eDisable = xpath.compile(".//FORM/DIV[@class='ozb']/INPUT[@type='submit']").evaluate(node, XPathConstants.NODE).asInstanceOf[Element]
      val isdisable = if (_eDisable != null) {false} else {true}

      if (isdisable){//商家停售
        println(s"merchant: ${_tMerchant}, is closed")

      }else{
        val catContentBuilder = BasicDBObjectBuilder.start()
        catContentBuilder.add("merchants_name", _tMerchant)
        catContentBuilder.add("bid", bid)
        var mMerchantContents: java.util.List[DBObject] = new util.ArrayList[DBObject]()
        catContentBuilder.add("contents", mMerchantContents)

        //获得荤素配下边的菜单
        //println("---------p-----\n" + nodeHTMLContent(node))
        var _nComboParent = xpath.compile(".//FORM/DIV[@class='mit clearfix'][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
        var _nCombos = xpath.compile("./DIV[contains(@class, 'm-i')]").evaluate(_nComboParent, XPathConstants.NODESET).asInstanceOf[NodeList]

        //mongo
        var mComboBuilder = BasicDBObjectBuilder.start()
        mComboBuilder.add("subcate_name", "combos")
        val mComboContents: java.util.List[DBObject] = new util.ArrayList[DBObject]()
        mComboBuilder.add("contents", mComboContents)

        for (_j <- 0 until _nCombos.getLength){
          val comboBuilder = BasicDBObjectBuilder.start()

          var node = _nCombos.item(_j)
          var comboName = xpath.compile("./H4/SPAN/text()").evaluate(node, XPathConstants.STRING).asInstanceOf[String]

          println(s" ---- >comboName is: $comboName")
          comboBuilder.add("combo_name", comboName)
          val _mfoods: java.util.List[DBObject] = new util.ArrayList[DBObject]()

          var _nfoods = xpath.compile(".//DIV[@class='foods']/LABEL/INPUT").evaluate(node, XPathConstants.NODESET).asInstanceOf[NodeList]
          for (_k <- 0 until _nfoods.getLength){
            var _eFood = _nfoods.item(_k).asInstanceOf[Element]
            var foodname = _eFood.getAttribute("cname")
            var formName = _eFood.getAttribute("name")
            var formValue = _eFood.getAttribute("value")

            println(s" food: $foodname, formName: $formName, formValue: $formValue")
            val foodBuilder = BasicDBObjectBuilder.start()
              .add("name", foodname).add("form_name", formName).add("form_value", formValue)
            _mfoods.add(foodBuilder.get())
          }

          comboBuilder.add("foods", _mfoods)
          mComboContents.add(comboBuilder.get())
        }
        mMerchantContents.add(mComboBuilder.get())

        //获得额外的主食订单
        var _nExtraMainFood = xpath.compile(".//FORM/DIV[@class='mt-o cf']").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]

        if (_nExtraMainFood != null){//荤素配下面的节点可能会为空
          var subcateName = xpath.compile("./DIV[@class='torange clearfix']/B/text()").evaluate(_nExtraMainFood, XPathConstants.STRING).asInstanceOf[String]
          subcateName = removeSpaces(subcateName)

          println("subcate name is : " + subcateName)
          mComboBuilder = BasicDBObjectBuilder.start()
          mComboBuilder.add("subcate_name", subcateName)
          val _mfoods: java.util.List[DBObject] = new util.ArrayList[DBObject]()
          mComboBuilder.add("foods", _mfoods)

          var _nSubcateFoods = xpath.compile("./UL/LI[@data-price]").evaluate(_nExtraMainFood, XPathConstants.NODESET).asInstanceOf[NodeList]
          for (_m <- 0 until _nSubcateFoods.getLength){
            var node = _nSubcateFoods.item(_m)
            // get xpath attibute
            var foodname = xpath.compile("./DIV[1]/@title").evaluate(node, XPathConstants.STRING).asInstanceOf[String]
            var price = node.asInstanceOf[Element].getAttribute("data-price")
            var _eInput = xpath.compile(".//INPUT[@type='text'][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Element]
            var fid = _eInput.getAttribute("fid")
            var bid = _eInput.getAttribute("bid")
            //这两个字段貌似只有在点荤素配的时候才有用
            var formName = _eInput.getAttribute("name")
            var formValue = _eInput.getAttribute("value")

            println(s"主食: $foodname, price: $price, fid: $fid, bid: $bid, formName: $formName, formValue: $formValue")
            val foodBuilder = BasicDBObjectBuilder.start()
              .add("name", foodname).add("form_name", formName).add("form_value", formValue).add("fid", fid).add("bid", bid)
              .add("price", price)
            _mfoods.add(foodBuilder.get())
          }
          mMerchantContents.add(mComboBuilder.get())
        }

        catContents.add(catContentBuilder.get())
      }
    }

    catContents
  }

  //其他类别的信息提取
  def parseCateB(ins: InputStream): java.util.List[DBObject] = {
    val catContents: java.util.List[DBObject] = new util.ArrayList[DBObject]()
    val document = getDocument(ins)

    //遍历每一家的菜品
    //选择最后一个class="bid-card "的div
    val cardsPath = "//DIV[contains(@class, 'bid-cards')]/DIV[@class='bid-card ']"
    val cardNodes: NodeList = xpath.compile(cardsPath).evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]

    println(s"商户节点: ${cardNodes.getLength} " )

    for (_i <- 0 until cardNodes.getLength()) {
      var node = cardNodes.item(_i)
      var _nMerchant: Node = xpath.compile(".//A[contains(@class, 'shop_aw')][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      //获得子类下的商户名
      var _tMerchant = removeSpaces(_nMerchant.getFirstChild().getTextContent())
      println(s"\n + sub cat merchant name is: ${_tMerchant} \n")

      val _eDisable = xpath.compile(".//FORM/DIV[@class='ozb']/INPUT[@type='button']").evaluate(node, XPathConstants.NODE).asInstanceOf[Element]
      val isdisable = if (_eDisable != null && _eDisable.getAttribute("disabled") != null && !_eDisable.getAttribute("disabled").isEmpty) {true} else {false}
      if (isdisable){//商家停售
        println(s"merchant: ${_tMerchant}, is closed")

      }else{
        val catContentBuilder = BasicDBObjectBuilder.start()
        catContentBuilder.add("merchants_name", _tMerchant)
        val merContents: java.util.List[DBObject] = new util.ArrayList[DBObject]()//商户对应的内容
        catContentBuilder.add("contents", merContents)

        //获得 食品子类
        var _nOrderZone: Node = xpath.compile(".//DIV[contains(@class, 'orderZone')][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
        var _nSubCats: NodeList = xpath.compile("./FORM/DIV").evaluate(_nOrderZone, XPathConstants.NODESET).asInstanceOf[NodeList]
        for (_j <- 0 until _nSubCats.getLength()) {
          var _nz = _nSubCats.item(_j)

          //食品子类的名称
          var _tSubCat: String = xpath.compile(".//DIV[contains(@class, 'torange')][1]/B/text()").evaluate(_nz, XPathConstants.STRING).asInstanceOf[String]
          _tSubCat = removeSpaces(_tSubCat)

          println(s"sub cat name under merchant ${_tMerchant} is : ${_tSubCat}")
          val merContentBuilder = BasicDBObjectBuilder.start()
          merContentBuilder.add("subcate_name", _tSubCat)
          val mFoods = new util.ArrayList[DBObject]()
          merContentBuilder.add("foods", mFoods)

          //食品
          val _nFoods: NodeList = xpath.compile(".//LI[@data-price]").evaluate(_nz, XPathConstants.NODESET).asInstanceOf[NodeList]

          for (_k <- 0 until _nFoods.getLength()) {
            var _nf: Node = _nFoods.item(_k)
            var price = _nf.asInstanceOf[Element].getAttribute("data-price")

            var fname = _nf.getChildNodes().item(1).asInstanceOf[Element].getAttribute("title")
            var _nfinfo: Element = xpath.compile("./DIV/INPUT[@fid][1]").evaluate(_nf, XPathConstants.NODE).asInstanceOf[Element]
            var fid = _nfinfo.getAttribute("fid")
            var bid = _nfinfo.getAttribute("bid")
            println(s" food name: $fname, fid: $fid, bid: $bid")

            val foodBuilder = BasicDBObjectBuilder.start()
            foodBuilder.add("name", fname).add("fid", fid).add("bid", bid).add("price", price)
            mFoods.add(foodBuilder.get())
          }

          merContents.add(merContentBuilder.get())
        }

        catContents.add(catContentBuilder.get())
      }

    }

    catContents
  }

  def removeSpaces(str: String): String = str.replaceAll("\\s+", " ")

  // inspect node html content
  def nodeHTMLContent(node: Node): String = {
    val tf = TransformerFactory.newInstance();
    val t = tf.newTransformer();
    t.setOutputProperty("encoding", "UTF-8"); // 解决中文问题，试过用GBK不行
    val bos = new ByteArrayOutputStream();
    t.transform(new DOMSource(node), new StreamResult(bos));
    return bos.toString();
  }
}
