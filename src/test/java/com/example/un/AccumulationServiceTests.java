package com.example.un;

import com.example.un.dto.AddCumulativeDto;
import com.example.un.models.Account;
import com.example.un.models.Cumulative;
import com.example.un.models.MountlyAccumulative;
import com.example.un.models.OperationType;
import com.example.un.repository.AccountRepository;
import com.example.un.repository.CumulativeRepository;
import com.example.un.repository.MountlyAccumulativeRepository;
import com.example.un.repository.ServiceRepository;
import com.example.un.services.AccumulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccumulationServiceTest {

    @Mock
    private CumulativeRepository cRepo;

    @Mock
    private AccountRepository accRepo;

    @Mock
    private ServiceRepository servRepo;

    @Mock
    private MountlyAccumulativeRepository mRepo;

    @InjectMocks
    private AccumulationService accumulationService;

    private Account account;
    private final Long ACCOUNT_ID = 1L;
    private final Long SERVICE_ID = 5L;
    private final BigDecimal SERVICE_COST = new BigDecimal("2.00");

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(ACCOUNT_ID);
        account.setBalance(new BigDecimal("1000.00"));
    }

    // CUMULATIVE_ADD — no existing monthly record

    @Test
    void addCumulative_add_noExistingMonthly_createsMonthlAndChargesBalance() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_ADD, new BigDecimal("10.00"), null);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), any()))
                .thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.empty());
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddCumulativeDto result = accumulationService.addCumulative(ACCOUNT_ID, dto);
        assertThat(account.getBalance()).isEqualByComparingTo("980.00");
        assertThat(result.getAmount()).isEqualByComparingTo("10.00"); // newVolume = 0 + 10
        assertThat(result.getOperationType()).isEqualTo(OperationType.CUMULATIVE_ADD);

        ArgumentCaptor<MountlyAccumulative> captor = ArgumentCaptor.forClass(MountlyAccumulative.class);
        verify(mRepo).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualByComparingTo("10.00");
        assertThat(captor.getValue().getMonthYear()).isEqualTo(startOfMonth);
    }

    @Test
    void addCumulative_add_withExistingMonthly_accumulatesVolume() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_ADD, new BigDecimal("5.00"), null);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        MountlyAccumulative existing = new MountlyAccumulative();
        existing.setValue(new BigDecimal("20.00"));
        existing.setMonthYear(startOfMonth);

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), any()))
                .thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.of(existing));
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accumulationService.addCumulative(ACCOUNT_ID, dto);
        assertThat(account.getBalance()).isEqualByComparingTo("990.00");
        assertThat(existing.getValue()).isEqualByComparingTo("25.00");
    }

    @Test
    void addCumulative_add_withPreviousMonthSnapshot_usesItAsBase() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_ADD, new BigDecimal("3.00"), null);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        MountlyAccumulative prev = new MountlyAccumulative();
        prev.setValue(new BigDecimal("50.00"));

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), any()))
                .thenReturn(Optional.of(prev));
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.empty());
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddCumulativeDto result = accumulationService.addCumulative(ACCOUNT_ID, dto);
        assertThat(account.getBalance()).isEqualByComparingTo("994.00");
        assertThat(result.getAmount()).isEqualByComparingTo("53.00");
    }

    // CUMULATIVE_SET — no existing monthly record

    @Test
    void addCumulative_set_noExisting_setsVolumeAndChargesFullDelta() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_SET, new BigDecimal("10.00"), null);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), any()))
                .thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.empty());
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accumulationService.addCumulative(ACCOUNT_ID, dto);

        assertThat(account.getBalance()).isEqualByComparingTo("980.00");
    }

    @Test
    void addCumulative_set_existingMonthly_chargesOnlyDelta() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_SET, new BigDecimal("25.00"), null);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        MountlyAccumulative existing = new MountlyAccumulative();
        existing.setValue(new BigDecimal("20.00"));

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), any()))
                .thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.of(existing));
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accumulationService.addCumulative(ACCOUNT_ID, dto);

        assertThat(account.getBalance()).isEqualByComparingTo("990.00");
        assertThat(existing.getValue()).isEqualByComparingTo("25.00");
    }

    @Test
    void addCumulative_set_lowerThanCurrent_creditsBalance() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_SET, new BigDecimal("10.00"), null);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        MountlyAccumulative existing = new MountlyAccumulative();
        existing.setValue(new BigDecimal("30.00"));

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), any()))
                .thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.of(existing));
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accumulationService.addCumulative(ACCOUNT_ID, dto);

        assertThat(account.getBalance()).isEqualByComparingTo("1040.00");
    }

    // Custom month (createAt field)

    @Test
    void addCumulative_withCustomCreateAt_usesProvidedMonth() {
        LocalDate customMonth = LocalDate.of(2024, 3, 1);
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_ADD, new BigDecimal("5.00"), customMonth);

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(eq(ACCOUNT_ID), eq(SERVICE_ID), eq(customMonth)))
                .thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, customMonth))
                .thenReturn(Optional.empty());
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accumulationService.addCumulative(ACCOUNT_ID, dto);

        ArgumentCaptor<MountlyAccumulative> captor = ArgumentCaptor.forClass(MountlyAccumulative.class);
        verify(mRepo).save(captor.capture());
        assertThat(captor.getValue().getMonthYear()).isEqualTo(customMonth);
    }

    // Account not found

    @Test
    void addCumulative_accountNotFound_throwsBadRequest() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_ADD, new BigDecimal("10.00"), null);

        when(accRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accumulationService.addCumulative(99L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("account has not found");

        verify(mRepo, never()).save(any());
        verify(cRepo, never()).save(any());
    }

    // Persistence calls are made

    @Test
    void addCumulative_add_savesAllEntities() {
        AddCumulativeDto dto = buildDto(OperationType.CUMULATIVE_ADD, new BigDecimal("1.00"), null);
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        when(accRepo.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(mRepo.findPreviousByAccountIdAndServiceId(any(), any(), any())).thenReturn(Optional.empty());
        when(mRepo.findByAccountIdAndServiceIdAndMonthYear(ACCOUNT_ID, SERVICE_ID, startOfMonth))
                .thenReturn(Optional.empty());
        when(servRepo.getServiceCostById(SERVICE_ID)).thenReturn(SERVICE_COST);
        when(cRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accumulationService.addCumulative(ACCOUNT_ID, dto);

        verify(mRepo).save(any(MountlyAccumulative.class));
        verify(cRepo).save(any(Cumulative.class));
        verify(accRepo).save(account);
    }


    private AddCumulativeDto buildDto(OperationType type, BigDecimal amount, LocalDate createAt) {
        AddCumulativeDto dto = new AddCumulativeDto();
        dto.setAccountId(ACCOUNT_ID);
        dto.setServiceId(SERVICE_ID);
        dto.setOperationType(type);
        dto.setAmount(amount);
        dto.setCreateAt(createAt);
        return dto;
    }
}