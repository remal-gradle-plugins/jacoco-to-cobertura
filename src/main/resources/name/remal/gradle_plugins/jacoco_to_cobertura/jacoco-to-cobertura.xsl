<?xml version="1.0" encoding = "UTF-8" standalone="yes"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

  <xsl:output method="xml" encoding="UTF-8" indent="no" standalone="yes"/>

  <xsl:template match="/report">
    <coverage>
      <xsl:attribute name="timestamp">
        <xsl:value-of select="'0'"/>
      </xsl:attribute>
      <xsl:if test="sessioninfo[1]/@start">
        <xsl:attribute name="timestamp">
          <xsl:value-of select="floor(number(sessioninfo[1]/@start) div 1000)"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:apply-templates select="current()" mode="counters"/>

      <packages>
        <xsl:apply-templates select="package"/>
      </packages>
    </coverage>
  </xsl:template>

  <xsl:template match="package">
    <package>
      <xsl:attribute name="name">
        <xsl:value-of select="@name"/>
      </xsl:attribute>

      <xsl:apply-templates select="current()" mode="counters"/>

      <classes>
        <xsl:apply-templates select="class"/>
      </classes>
    </package>
  </xsl:template>

  <xsl:template match="class">
    <class>
      <xsl:attribute name="name">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <xsl:attribute name="filename">
        <xsl:value-of select="concat(parent::package/@name, '/', @sourcefilename)"/>
      </xsl:attribute>

      <xsl:apply-templates select="current()" mode="counters"/>

      <methods>
        <xsl:apply-templates select="method"/>
      </methods>
    </class>
  </xsl:template>

  <xsl:template match="method">
    <method>
      <xsl:attribute name="name">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <xsl:attribute name="signature">
        <xsl:value-of select="@desc"/>
      </xsl:attribute>
      <xsl:apply-templates select="current()" mode="counters"/>


      <xsl:variable name="source-file" select="../@sourcefilename"/>

      <xsl:variable name="line-from" select="number(@line)"/>

      <xsl:variable name="sibling-method-lines" select="../method[generate-id(.) != generate-id(current())]/@line[number(.) > $line-from]"/>
      <xsl:variable name="next-to-excluding">
        <xsl:choose>
          <xsl:when test="$sibling-method-lines">
            <xsl:for-each select="$sibling-method-lines">
              <xsl:sort select="." data-type="number"/>
              <xsl:if test="position() = 1">
                <xsl:value-of select="."/>
              </xsl:if>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="2147483647"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:variable name="line-nodes" select="../../sourcefile[@name = $source-file]/line[number(@nr) >= $line-from and $next-to-excluding > number(@nr)]"/>
      <xsl:if test="$line-nodes">
        <lines>
          <xsl:for-each select="$line-nodes">
            <xsl:sort select="@line" data-type="number"/>
            <line branch="false">
              <xsl:attribute name="number">
                <xsl:value-of select="./@nr"/>
              </xsl:attribute>

              <xsl:attribute name="hits">
                <xsl:value-of select="'0'"/>
              </xsl:attribute>
              <xsl:if test="number(./@ci) > 0">
                <xsl:attribute name="hits">
                  <xsl:value-of select="'1'"/>
                </xsl:attribute>
              </xsl:if>

              <xsl:if test="number(@mb) + number(@cb) > 0">
                <xsl:attribute name="branch">
                  <xsl:value-of select="'true'"/>
                </xsl:attribute>

                <xsl:variable name="condition-coverage-percent" select="floor(100 * number(@cb) div (number(@mb) + number(@cb)))"/>
                <xsl:attribute name="condition-coverage">
                  <xsl:value-of select="concat($condition-coverage-percent, '% (', @cb, '/', number(@mb) + number(@cb), ')')"/>
                </xsl:attribute>
                <conditions class="java.util.Collections$SingletonList">
                  <condition number="0" type="jump">
                    <xsl:attribute name="coverage">
                      <xsl:value-of select="concat($condition-coverage-percent, '%')"/>
                    </xsl:attribute>
                  </condition>
                </conditions>
              </xsl:if>
            </line>
          </xsl:for-each>
        </lines>
      </xsl:if>
    </method>
  </xsl:template>


  <xsl:template match="*" mode="counters">
    <xsl:attribute name="branch-rate">
      <xsl:value-of select="'0'"/>
    </xsl:attribute>
    <xsl:if test="
      counter[@type='BRANCH']/@missed
      and counter[@type='BRANCH']/@covered
      and counter[@type='BRANCH']/@covered != 0
    ">
      <xsl:attribute name="branch-rate">
        <xsl:value-of select="
          number(counter[@type='BRANCH']/@covered)
          div (number(counter[@type='BRANCH']/@missed) + number(counter[@type='BRANCH']/@covered))
        "/>
      </xsl:attribute>
    </xsl:if>

    <xsl:attribute name="line-rate">
      <xsl:value-of select="'0'"/>
    </xsl:attribute>
    <xsl:if test="
      counter[@type='LINE']/@missed
      and counter[@type='LINE']/@covered
      and number(counter[@type='LINE']/@covered) != 0
    ">
      <xsl:attribute name="line-rate">
        <xsl:value-of select="
          number(counter[@type='LINE']/@covered)
          div (number(counter[@type='LINE']/@missed) + number(counter[@type='LINE']/@covered))
        "/>
      </xsl:attribute>
    </xsl:if>

    <xsl:attribute name="complexity">
      <xsl:value-of select="'0'"/>
    </xsl:attribute>
    <xsl:if test="
      counter[@type='COMPLEXITY']/@missed
      and counter[@type='COMPLEXITY']/@covered
    ">
      <xsl:attribute name="complexity">
        <xsl:value-of select="
          number(counter[@type='COMPLEXITY']/@missed)
          + number(counter[@type='COMPLEXITY']/@covered)
        "/>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
