<ctl:package
 xmlns:ctl="http://www.occamlab.com/ctl"
 xmlns:ctlp="http://www.occamlab.com/te/parsers"
 xmlns:fn="http://www.w3.org/2005/xpath-functions"
 xmlns:wms="http://www.opengis.net/wms"
 xmlns:xlink="http://www.w3.org/1999/xlink"
 xmlns:xhtml="http://www.w3.org/1999/xhtml"
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:main="urn:wms_client_test_suite/main"
 xmlns:basic="urn:wms_client_test_suite/basic_elements"
 xmlns:gc="urn:wms_client_test_suite/GetCapabilities"
 xmlns:gm="urn:wms_client_test_suite/GetMap"
 xmlns:gfi="urn:wms_client_test_suite/GetFeatureInfo"
>
    <ctl:suite name="main:suite">
        <ctl:title>Partial WMS Client Test Suite</ctl:title>
        <ctl:description>Validates WMS Client Requests.</ctl:description>
        <ctl:starting-test>main:root</ctl:starting-test>
        <ctl:form>
            <xsl:text>Enter the Capabilities URL:</xsl:text>
            <xhtml:br/>
            <xhtml:input name="capabilities-url" size="100" type="text" value=""/>
            <xhtml:br/>
            <xhtml:input type="submit" value="OK"/>
        </ctl:form>
    </ctl:suite>

    <ctl:test name="main:root">
        <ctl:param name="capabilities-url"/>
        <ctl:assertion>The WMS client is valid.</ctl:assertion>
        <ctl:code>
            <xsl:variable name="capabilities">
                <ctl:request>
                    <ctl:url>
                        <xsl:value-of select="$capabilities-url"/>
                    </ctl:url>
                </ctl:request>
            </xsl:variable>

            <xsl:variable name="monitor-urls">
                <xsl:for-each select="$capabilities/wms:WMS_Capabilities/wms:Capability/wms:Request">
                    <xsl:for-each select="wms:GetCapabilities|wms:GetMap|wms:GetFeatureInfo">
                        <xsl:copy>
                            <ctl:allocate-monitor-url>
                                <xsl:value-of select="wms:DCPType/wms:HTTP/wms:Get/wms:OnlineResource/@xlink:href"/>
                            </ctl:allocate-monitor-url>
                        </xsl:copy>
                    </xsl:for-each>
                </xsl:for-each>
            </xsl:variable>
            
            <ctl:create-monitor>
                <ctl:url>
                    <xsl:value-of select="$monitor-urls/wms:GetCapabilities"/>
                </ctl:url>
                <ctl:triggers-test name="gc:check-GetCapabilities-request">
                    <ctl:with-param name="capabilities" select="$capabilities/wms:WMS_Capabilities"/>
                </ctl:triggers-test>
                <ctl:with-parser modifies-response="true">
                    <ctlp:XSLTransformationParser resource="rewrite-capabilities.xsl">
                        <ctlp:with-param name="GetCapabilities-proxy">
                            <xsl:value-of select="$monitor-urls/wms:GetCapabilities"/>
                        </ctlp:with-param>
                        <ctlp:with-param name="GetMap-proxy">
                            <xsl:value-of select="$monitor-urls/wms:GetMap"/>
                        </ctlp:with-param>
                        <ctlp:with-param name="GetFeatureInfo-proxy">
                            <xsl:value-of select="$monitor-urls/wms:GetFeatureInfo"/>
                        </ctlp:with-param>
                    </ctlp:XSLTransformationParser>
                </ctl:with-parser>
            </ctl:create-monitor>

            <ctl:create-monitor>
                <ctl:url>
                    <xsl:value-of select="$monitor-urls/wms:GetMap"/>
                </ctl:url>
                <ctl:triggers-test name="gm:check-GetMap-request">
                    <ctl:with-param name="capabilities" select="$capabilities/wms:WMS_Capabilities"/>
                </ctl:triggers-test>
                <ctl:with-parser>
                    <ctlp:NullParser/>
                </ctl:with-parser>
            </ctl:create-monitor>

            <ctl:create-monitor>
                <ctl:url>
                    <xsl:value-of select="$monitor-urls/wms:GetFeatureInfo"/>
                </ctl:url>
                <ctl:triggers-test name="gfi:check-GetFeatureInfo-request">
                    <ctl:with-param name="capabilities" select="$capabilities/wms:WMS_Capabilities"/>
                </ctl:triggers-test>
                <ctl:with-parser>
                    <ctlp:NullParser/>
                </ctl:with-parser>
            </ctl:create-monitor>

            <ctl:form>
                <xsl:text>Configure the Client to use this URL:</xsl:text>
                <xhtml:br/>
                <xsl:value-of select="$monitor-urls/wms:GetCapabilities"/>
                <xhtml:br/>
                <xhtml:br/>
                <xsl:text>Leave this form open while you use the client.</xsl:text>
                <xhtml:br/>
                <xsl:text>Press the button when you are finished.</xsl:text>
                <xhtml:br/>
                <xhtml:input type="submit" value="Done Testing"/>
            </ctl:form>
        </ctl:code>
    </ctl:test>
    
    <ctl:function name="main:parse-list">
        <ctl:param name="list"/>
        <ctl:code>
            <xsl:choose>
                <xsl:when test="contains($list, ',')">
                    <value>
                        <xsl:value-of select="substring-before($list, ',')"/>
                    </value>
                    <ctl:call-function name="main:parse-list">
                        <ctl:with-param name="list" select="substring-after($list, ',')"/>
                    </ctl:call-function>
                </xsl:when>
                <xsl:otherwise>
                    <value>
                        <xsl:value-of select="$list"/>
                    </value>
                </xsl:otherwise>
            </xsl:choose>
        </ctl:code>
    </ctl:function>
</ctl:package>
