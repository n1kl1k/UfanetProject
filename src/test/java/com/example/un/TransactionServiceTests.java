package com.example.un;

import com.example.un.dto.CreateOperDto;
import com.example.un.dto.RefactorOperDto;
import com.example.un.models.Account;
import com.example.un.models.Operation;
import com.example.un.models.OperationType;
import com.example.un.models.SnapshotBalance;
import com.example.un.repository.AccountRepository;
import com.example.un.repository.OperationRepository;
import com.example.un.repository.SnapshotBalanceRepository;
import com.example.un.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accRepo;

    @Mock
    private OperationRepository operRepo;

    @Mock
    private SnapshotBalanceRepository sBRepo;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private Operation operation;


    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal("1000.00"));

        operation = new Operation();
        operation.setId(10L);
        operation.setAccountId(1L);
        operation.setAmount(new BigDecimal("200.00"));
        operation.setOperationType(OperationType.PAYMENT);
        operation.setCreatedAt(LocalDateTime.now());
    }

    //Create base operation Payment

    //тест пополнения
    @Test
    void createBaseOper_payment_increasesBalance() {
        CreateOperDto dto = new CreateOperDto();
        dto.setAmount(new BigDecimal("300.00"));
        dto.setOperationType(OperationType.PAYMENT);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.save(any())).thenAnswer(inv -> {
            Operation op = inv.getArgument(0);
            op.setId(10L);
            return op;
        });

        CreateOperDto result = transactionService.createBaseOper(1L, dto);

        assertThat(account.getBalance()).isEqualByComparingTo("1300.00");
        assertThat(result.getOperationType()).isEqualTo(OperationType.PAYMENT);
        assertThat(result.getAmount()).isEqualByComparingTo("300.00");
        verify(accRepo).save(account);
    }

    //тест пополнение на 0
    @Test
    void createBaseOper_payment_zeroAmount_throwsBadRequest() {
        CreateOperDto dto = new CreateOperDto();
        dto.setAmount(BigDecimal.ZERO);
        dto.setOperationType(OperationType.PAYMENT);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createBaseOper(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("the replenishment cannot be greater than zero");
    }

    //пополнение отрицательной суммы
    @Test
    void createBaseOper_payment_negativeAmount_throwsBadRequest() {
        CreateOperDto dto = new CreateOperDto();
        dto.setAmount(new BigDecimal("-50.00"));
        dto.setOperationType(OperationType.PAYMENT);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createBaseOper(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("the replenishment cannot be greater than zero");
    }

    //Create base operation CHARGE

    //описание списания
    @Test
    void createBaseOper_charge_decreasesBalance() {
        CreateOperDto dto = new CreateOperDto();
        dto.setAmount(new BigDecimal("400.00"));
        dto.setOperationType(OperationType.CHARGE);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.save(any())).thenAnswer(inv -> {
            Operation op = inv.getArgument(0);
            op.setId(10L);
            return op;
        });

        transactionService.createBaseOper(1L, dto);

        assertThat(account.getBalance()).isEqualByComparingTo("600.00");
    }

    //списание на сумму больше чем баланс
    @Test
    void createBaseOper_charge_insufficientFunds_throwsBadRequest() {
        CreateOperDto dto = new CreateOperDto();
        dto.setAmount(new BigDecimal("2000.00"));
        dto.setOperationType(OperationType.CHARGE);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> transactionService.createBaseOper(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("insufficient funds");
    }

    //Refactor operation
    //проверка на правильный пересчёт баланса при изменении PAYMENT
    @Test
    void refactorBaseOper_changePaymentAmount_updatesBalance() {
        // Old: PAYMENT 200 → balance was +200; New: PAYMENT 500 → delta = +300
        RefactorOperDto dto = new RefactorOperDto();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setOperationType(OperationType.PAYMENT);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.findById(10L)).thenReturn(Optional.of(operation));
        when(operRepo.save(any())).thenReturn(operation);

        transactionService.refactorBaseOper(dto, 1L, 10L);
        assertThat(account.getBalance()).isEqualByComparingTo("1300.00");
    }

    //Проверка на изменение баланса при изменении типа операции
    @Test
    void refactorBaseOper_changePaymentToCharge_updatesBalance() {
        RefactorOperDto dto = new RefactorOperDto();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setOperationType(OperationType.CHARGE);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.findById(10L)).thenReturn(Optional.of(operation));
        when(operRepo.save(any())).thenReturn(operation);

        transactionService.refactorBaseOper(dto, 1L, 10L);
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
    }
    //проверка на операции, которые приведут к отрицательному счёту
    @Test
    void refactorBaseOper_wouldResultInNegativeBalance_throwsBadRequest() {
        RefactorOperDto dto = new RefactorOperDto();
        dto.setAmount(new BigDecimal("2000.00"));
        dto.setOperationType(OperationType.CHARGE);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.findById(10L)).thenReturn(Optional.of(operation));

        assertThatThrownBy(() -> transactionService.refactorBaseOper(dto, 1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("insufficient funds after operation change");
    }
    //проверка на существование счёта
    @Test
    void refactorBaseOper_accountNotFound_throwsBadRequest() {
        RefactorOperDto dto = new RefactorOperDto();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setOperationType(OperationType.PAYMENT);

        when(accRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.refactorBaseOper(dto, 99L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("account id didn`t found");
    }
    //проверка на существование операции
    @Test
    void refactorBaseOper_operationNotFound_throwsBadRequest() {
        RefactorOperDto dto = new RefactorOperDto();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setOperationType(OperationType.PAYMENT);

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.refactorBaseOper(dto, 1L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("operation is doesn`t exist");
    }

    //Delete operation
    //проверка на правильный откат пополнения
    @Test
    void deleteBaseOper_payment_subtractsFromBalance() {
        // operation is PAYMENT 200 → deleting it should subtract 200
        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.findById(10L)).thenReturn(Optional.of(operation));

        transactionService.deleteBaseOper(1L, 10L);

        assertThat(account.getBalance()).isEqualByComparingTo("800.00");
        verify(operRepo).deleteById(10L);
        verify(accRepo).save(account);
    }
    //проверка на правильный откат списания
    @Test
    void deleteBaseOper_charge_addsBackToBalance() {
        operation.setOperationType(OperationType.CHARGE);
        operation.setAmount(new BigDecimal("150.00"));

        when(accRepo.findById(1L)).thenReturn(Optional.of(account));
        when(operRepo.findById(10L)).thenReturn(Optional.of(operation));

        transactionService.deleteBaseOper(1L, 10L);

        assertThat(account.getBalance()).isEqualByComparingTo("1150.00");
        verify(operRepo).deleteById(10L);
    }

    //Get balance on date
    //проверка на правильность использование снепшота
    @Test
    void getBalanceOnDate_withSnapshot_usesSnapshotAsBase() {
        LocalDate targetDate = LocalDate.of(2024, 6, 15);

        SnapshotBalance snapshot = new SnapshotBalance();
        snapshot.setBalance(new BigDecimal("500.00"));
        snapshot.setSnapshotDate(LocalDate.of(2024, 6, 10));

        when(sBRepo.findTopByAccountIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(1L, targetDate))
                .thenReturn(Optional.of(snapshot));
        when(operRepo.sumAmountByAccountAndDateRange(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("100.00"));

        BigDecimal result = transactionService.getBalanceOnDate(1L, targetDate);

        assertThat(result).isEqualByComparingTo("600.00");
    }
    //проверка на округление
    @Test
    void getBalanceOnDate_resultIsRoundedToTwoDecimalPlaces() {
        LocalDate targetDate = LocalDate.of(2024, 6, 15);

        SnapshotBalance snapshot = new SnapshotBalance();
        snapshot.setBalance(new BigDecimal("100.00"));
        snapshot.setSnapshotDate(LocalDate.of(2024, 6, 1));

        when(sBRepo.findTopByAccountIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(1L, targetDate))
                .thenReturn(Optional.of(snapshot));
        when(operRepo.sumAmountByAccountAndDateRange(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("0.555"));

        BigDecimal result = transactionService.getBalanceOnDate(1L, targetDate);

        assertThat(result).isEqualByComparingTo("100.56"); // HALF_UP
        assertThat(result.scale()).isEqualTo(2);
    }
}