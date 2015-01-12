package demo

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.StringWriter

import org.cyberneko.html.parsers.DOMParser
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * @author miaojing
 * email 243127395@qq.com
 *
 * HTML XPATH 解析的demo类
 *
 * 目标网页:
 * 		http://fuhua.xiaozufan.com/
 *
 * #tools
 *      [xpath onlinie tester](http://xpath.online-toolz.com/tools/xpath-editor.php)
 *
 * #refrence
 *      [NekoHTML xml](http://dustin.iteye.com/blog/286232)
 *      [java xml and xpath](http://viralpatel.net/blogs/java-xml-xpath-tutorial-parse-xml/)
 *      [java xpath api](http://www.ibm.com/developerworks/cn/xml/x-javaxpathapi.html)
 */


object xpathdemo {
  def main(args: Array[String]) {
    cate_a()
  }


  def cate_a(): Unit = {
    // fetch an inputsource
    val inputStream = this.getClass().getResourceAsStream("/cate_a.html")
    val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
    val bufferReader = new BufferedReader(inputStreamReader)

    //  val source = Source.fromInputStream(inputStream, "utf-8")
    //  for (line <- source.getLines){
    //    println(line)
    //  }
    //

    //set up nekohtml parser
    val parser = new DOMParser()
    val inputSource = new InputSource()
    inputSource.setCharacterStream(inputStreamReader);

    //configure parser

    /*The Xerces HTML DOM implementation does not support namespaces
    and cannot represent XHTML documents with namespace information.
    Therefore, in order to use the default HTML DOM implementation with NekoHTML's
    DOMParser to parse XHTML documents, you must turn off namespace processing.*/
    parser.setFeature("http://xml.org/sax/features/namespaces", false);

    //    val elementRemover = new ElementRemover()
    //    elementRemover.acceptElement("html", null);
    //    elementRemover.acceptElement("body", null);
    //    elementRemover.acceptElement("header", null);
    //    elementRemover.acceptElement("div", null);
    //    elementRemover.acceptElement("meta", Array("name", "content"));
    //    elementRemover.acceptElement("title", null);
    //
    //    elementRemover.acceptElement("base", Array("href"));
    //    elementRemover.acceptElement("b", null);
    //    elementRemover.acceptElement("i", null);
    //    elementRemover.acceptElement("u", null);
    //    elementRemover.acceptElement("p", null);
    //    elementRemover.acceptElement("br", null);
    //    elementRemover.acceptElement("a", Array("href", "rel"));
    //    elementRemover.removeElement("script")
    //    elementRemover.removeElement("style");
    //    val filters = Array(elementRemover, writer);

    val sw = new StringWriter();
    //org.cyberneko.html.filters.Writer
    val writer = new org.cyberneko.html.filters.Writer(sw, "UTF-8")
    val filters = Array(writer);
    parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

    parser.parse(inputSource)

    //println(sw.toString())

    // get document
    val document: Document = parser.getDocument()


    //    println(document.getChildNodes().item(0).getChildNodes().item(0).getNodeName())

    //如何正确结合NekoHTML和XPath XPath的Tag必须大写
    val xpath: XPath = XPathFactory.newInstance().newXPath()
    val catPath = "//DIV[contains(@class, 'caileinav')]/DIV[contains(@class, 'cailei')]//LI/A"

    //查找菜品的类别
    val nodeList: NodeList = xpath.compile(catPath).evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]
    for (_i <- 0 until nodeList.getLength()) {
      val node: Node = nodeList.item(_i)
      // println(node.getTextContent)
    }

    //遍历每一家的菜品
    //选择最后一个class="bid-card "的div
    val cardsPath = "//DIV[contains(@class, 'bid-cards')]/DIV[@class='bid-card ']"
    val cardNodes: NodeList = xpath.compile(cardsPath).evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]

    for (_i <- 0 until cardNodes.getLength) {
      var node = cardNodes.item(_i)
      var _nMerchant: Node = xpath.compile(".//A[contains(@class, 'shop_aw')][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      //获得子类下的商户名
      var _tMerchant = _nMerchant.getFirstChild().getTextContent()
      println(s"\n+ sub cat merchant name is: ${_tMerchant} \n")

      //获得荤素配下边的菜单
      var _nComboParent = xpath.compile(".//FORM/DIV[@class='mit clearfix'][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      var _nCombos = xpath.compile("./DIV[contains(@class, 'm-i')]").evaluate(_nComboParent, XPathConstants.NODESET).asInstanceOf[NodeList]
      for (_j <- 0 until _nCombos.getLength){
        var node = _nCombos.item(_j)
        var comboName = xpath.compile("./H4/SPAN/text()").evaluate(node, XPathConstants.STRING).asInstanceOf[String]
        println(s" ---- >comboName is: $comboName")
        var _nfoods = xpath.compile(".//DIV[@class='foods']/LABEL/INPUT").evaluate(node, XPathConstants.NODESET).asInstanceOf[NodeList]
        for (_k <- 0 until _nfoods.getLength){
          var _eFood = _nfoods.item(_k).asInstanceOf[Element]
          var foodname = _eFood.getAttribute("cname")
          var formName = _eFood.getAttribute("name")
          var formValue = _eFood.getAttribute("value")
          println(s" food: $foodname, formName: $formName, formValue: $formValue")
        }
      }

      //获得额外的主食订单
      var _nExtraMainFood = xpath.compile(".//FORM/DIV[@class='mt-o cf']").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      var subcateName = xpath.compile("./DIV[@class='torange clearfix']/B/text()").evaluate(_nExtraMainFood, XPathConstants.STRING).asInstanceOf[String]
      subcateName = removeSpaces(subcateName)
      println("subcate name is : " + subcateName)

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

      }
    }
  }

  def cate_b(): Unit ={
    // fetch an inputsource
    val inputStream = this.getClass().getResourceAsStream("/cate_b.html")
    val inputStreamReader = new InputStreamReader(inputStream, "utf-8")
    val bufferReader = new BufferedReader(inputStreamReader)

    //  val source = Source.fromInputStream(inputStream, "utf-8")
    //  for (line <- source.getLines){
    //    println(line)
    //  }
    //

    //set up nekohtml parser
    val parser = new DOMParser()
    val inputSource = new InputSource()
    inputSource.setCharacterStream(inputStreamReader);

    //configure parser

    /*The Xerces HTML DOM implementation does not support namespaces
    and cannot represent XHTML documents with namespace information.
    Therefore, in order to use the default HTML DOM implementation with NekoHTML's
    DOMParser to parse XHTML documents, you must turn off namespace processing.*/
    parser.setFeature("http://xml.org/sax/features/namespaces", false);

    //    val elementRemover = new ElementRemover()
    //    elementRemover.acceptElement("html", null);
    //    elementRemover.acceptElement("body", null);
    //    elementRemover.acceptElement("header", null);
    //    elementRemover.acceptElement("div", null);
    //    elementRemover.acceptElement("meta", Array("name", "content"));
    //    elementRemover.acceptElement("title", null);
    //
    //    elementRemover.acceptElement("base", Array("href"));
    //    elementRemover.acceptElement("b", null);
    //    elementRemover.acceptElement("i", null);
    //    elementRemover.acceptElement("u", null);
    //    elementRemover.acceptElement("p", null);
    //    elementRemover.acceptElement("br", null);
    //    elementRemover.acceptElement("a", Array("href", "rel"));
    //    elementRemover.removeElement("script")
    //    elementRemover.removeElement("style");
    //    val filters = Array(elementRemover, writer);

    val sw = new StringWriter();
    //org.cyberneko.html.filters.Writer
    val writer = new org.cyberneko.html.filters.Writer(sw, "UTF-8")
    val filters = Array(writer);
    parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

    parser.parse(inputSource)

    //println(sw.toString())

    // get document
    val document: Document = parser.getDocument()


    //    println(document.getChildNodes().item(0).getChildNodes().item(0).getNodeName())

    //如何正确结合NekoHTML和XPath XPath的Tag必须大写
    val xpath: XPath = XPathFactory.newInstance().newXPath()
    val catPath = "//DIV[contains(@class, 'caileinav')]/DIV[contains(@class, 'cailei')]//LI/A"

    //查找菜品的类别
    val nodeList: NodeList = xpath.compile(catPath).evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]
    for (_i <- 0 until nodeList.getLength()) {
      val node: Node = nodeList.item(_i)
      // println(node.getTextContent)
    }

    //遍历每一家的菜品
    //选择最后一个class="bid-card "的div
    val cardsPath = "//DIV[contains(@class, 'bid-cards')]/DIV[@class='bid-card ']"
    val cardNodes: NodeList = xpath.compile(cardsPath).evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]

    for (_i <- 0 until cardNodes.getLength()) {
      var node = cardNodes.item(_i)
      var _nMerchant: Node = xpath.compile(".//A[contains(@class, 'shop_aw')][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      //获得子类下的商户名
      var _tMerchant = removeSpaces(_nMerchant.getFirstChild().getTextContent())
      println(s"\n + sub cat merchant name is: ${_tMerchant} \n")

      //获得 食品子类
      var _nOrderZone: Node = xpath.compile(".//DIV[contains(@class, 'orderZone')][1]").evaluate(node, XPathConstants.NODE).asInstanceOf[Node]
      var _nSubCats: NodeList = xpath.compile("./FORM/DIV").evaluate(_nOrderZone, XPathConstants.NODESET).asInstanceOf[NodeList]
      for (_j <- 0 until _nSubCats.getLength()) {
        var _nz = _nSubCats.item(_j)

        //食品子类的名称
        var _tSubCat: String = xpath.compile(".//DIV[contains(@class, 'torange')][1]/B/text()").evaluate(_nz, XPathConstants.STRING).asInstanceOf[String]
        _tSubCat = removeSpaces(_tSubCat)

        println(s"sub cat name under merchant ${_tMerchant} is : ${_tSubCat}")

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
        }
      }
    }
  }




  // inspect node html content
  def nodeHTMLContent(node: Node): String = {
    val tf = TransformerFactory.newInstance();
    val t = tf.newTransformer();
    t.setOutputProperty("encoding", "UTF-8"); // 解决中文问题，试过用GBK不行  
    val bos = new ByteArrayOutputStream();
    t.transform(new DOMSource(node), new StreamResult(bos));
    return bos.toString();
  }

  def removeSpaces(str: String): String = str.replaceAll("\\s+", " ")
}