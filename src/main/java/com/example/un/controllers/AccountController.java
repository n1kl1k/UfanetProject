package com.example.un.controllers;

import com.example.un.dto.*;
import com.example.un.models.Account;
import com.example.un.repository.AccountRepository;
import com.example.un.repository.OperationRepository;
import com.example.un.services.AccumulationService;
import com.example.un.services.MessageProduserService;
import com.example.un.services.TransactionService;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/account/")
@RequiredArgsConstructor
public class AccountController {
    private final TransactionService trService;
    private final AccountRepository accRepo;
    private final AccumulationService accumulationService;
    private final MessageProduserService producer;

    //Создание операции
    @PostMapping("/{accountId}/transaction")
    public CreateOperDto createOperDto(@PathVariable Long accountId,
                                       @RequestBody CreateOperDto dto){
        return trService.createBaseOper(accountId, dto);
    }

    //Редактирование операции
    @PutMapping("/{accountId}/transaction/{operationId}")
    public RefactorOperDto refactorOperDto(@PathVariable Long accountId,
                                           @PathVariable Long operationId,
                                           @RequestBody RefactorOperDto dto){
        return trService.refactorBaseOper(dto,accountId,operationId);
    }

    //Удаление операции
    @DeleteMapping("/{accountId}/transaction/{operationId}")
    public String deleteOperation(@PathVariable Long accountId,
                                    @PathVariable Long operationId){
        trService.deleteBaseOper(accountId,operationId);
        return "Operation " + operationId + " on user, with id " + accountId + " has been deleted";
    }

    //Получение текущего баланса по id юзера
    @GetMapping("/{accountId}/currentBalance")
    public String getBalanceUser(@PathVariable Long accountId){
        return "Balance in account: " + accRepo.getBalanceById(accountId) + " where id: " + accountId;
    }

    //Получить баланс на нужную дату конкретного юзера
    @GetMapping("/{accountId}/balance/history")
    public ResponseEntity<BalanceResponse> getBalanceOnDate(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BigDecimal balance = trService.getBalanceOnDate(accountId, date);
        return ResponseEntity.ok(new BalanceResponse(accountId, balance, date));
    }

    @PostMapping("/{accountId}/accumulative")
    public AddCumulativeDto addCumulativeDt(@PathVariable Long accountId,
                                            @RequestBody AddCumulativeDto dto){
        return accumulationService.addCumulative(accountId,dto);
    }


    @PostMapping("/{accountId}/transaction/async")
    public ResponseEntity<String> createTransactionAsync(@PathVariable Long accountId,
                                                         @RequestBody TransactionMessage message) {
        message.setAccountId(accountId);
        producer.sendTransaction(message);
        return ResponseEntity.accepted().body("Accepted: " + message.getReferenceId());
    }

    @PostMapping("/{accountId}/accumulative/async")
    public ResponseEntity<String> addCumulativeAsync(@PathVariable Long accountId,
                                                     @RequestBody AccumulativeMessage message) {
        message.setAccountId(accountId);
        producer.sendAccumulative(message);
        return ResponseEntity.accepted().body("Accepted: " + message.getReferenceId());
    }

}
