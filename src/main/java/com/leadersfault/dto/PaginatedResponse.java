package com.leadersfault.dto;

import java.util.List;

public class PaginatedResponse<T> {

  private List<T> content;
  private PaginationInfo pagination;

  public PaginatedResponse(List<T> content, int pageCount) {
    this.content = content;
    this.pagination = new PaginationInfo(pageCount);
  }

  public List<T> getContent() {
    return content;
  }

  public void setContent(List<T> content) {
    this.content = content;
  }

  public PaginationInfo getPagination() {
    return pagination;
  }

  public void setPagination(PaginationInfo pagination) {
    this.pagination = pagination;
  }

  public static class PaginationInfo {

    private int pageCount;

    public PaginationInfo(int pageCount) {
      this.pageCount = pageCount;
    }

    public int getPageCount() {
      return pageCount;
    }

    public void setPageCount(int pageCount) {
      this.pageCount = pageCount;
    }
  }
}
