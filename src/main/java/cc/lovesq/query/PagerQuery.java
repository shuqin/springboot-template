package cc.lovesq.query;

import lombok.Data;

import java.io.Serializable;

@Data
public class PagerQuery implements Serializable {

  private static final long serialVersionUID = 3567389263772243142L;

  /**
   * 开始的记录行
   **/
  private Integer firstRow = 0;

  /**
   * 默认一页20行
   **/
  private Integer pageSize = 20;

  /**
   * 当前页
   **/
  private int pageNum = 1;


}
