<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">
    <div class="col-md-8 col-md-offset-2 padding-top">
        <#list requests as request>
            <div class="well">
                <form action="/transact" method="post" id="transactform">
                    <h2>${request.touser} has requested $${request.amount}.</h2>
                    <p>${request.message}</p>
                    <div class="input-group">
                        <input type="hidden" name="recipient" value="${request.touser}">
                        <input type="hidden" name="tranid" value="${request.tranid}">
                        <input type="hidden" name="amount" value="${request.amount}">
                        <input type="hidden" name="message" value="${request.message}">

                        <input type="password" id="transaction-password"
                               class="form-control" placeholder="Password">
                          <span class="input-group-btn">
                            <button class="btn btn-secondary" type="submit">Accept</button>
                          </span>
                    </div>

                </form>
            </div>
        </#list>


    </div>

</div>
<!-- /.container -->
</@layout.master>

