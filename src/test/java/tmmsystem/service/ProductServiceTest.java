package tmmsystem.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tmmsystem.entity.Product;
import tmmsystem.repository.ProductRepository;
import tmmsystem.repository.MaterialRepository;
import tmmsystem.repository.BomRepository;
import tmmsystem.repository.BomDetailRepository;
import tmmsystem.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases cho ProductService:
 *
 * 1. getProduct()  -> 4 test cases
 * 2. listProducts() -> 2 test cases
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepo;
    @Mock private MaterialRepository materialRepo;
    @Mock private BomRepository bomRepo;
    @Mock private BomDetailRepository bomDetailRepo;

    @InjectMocks
    private ProductService service;

    // ======================================================================
    // 🔵 TEST getProduct(id)
    // ======================================================================
    @Nested
    @DisplayName("getProduct() Tests")
    class GetProductTests {

        @Test
        @DisplayName("UTC01 - Normal: id = 1 → return product")
        void getProduct_Normal() {
            Product p = new Product();
            p.setId(1L);
            p.setName("Test Product");

            when(productRepo.findById(1L)).thenReturn(Optional.of(p));

            Product result = service.getProduct(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Product", result.getName());
            verify(productRepo, times(1)).findById(1L);
        }

        @Test
        @DisplayName("UTC02 - Abnormal: id = 999 (not found) → throw exception")
        void getProduct_NotFound() {
            when(productRepo.findById(999L)).thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> service.getProduct(999L));
            verify(productRepo, times(1)).findById(999L);
        }

        @Test
        @DisplayName("UTC03 - Boundary: id = -1 → not found → throw exception")
        void getProduct_IdNegative() {
            when(productRepo.findById(-1L)).thenReturn(Optional.empty());

            assertThrows(Exception.class, () -> service.getProduct(-1L));
            verify(productRepo, times(1)).findById(-1L);
        }

        @Test
        @DisplayName("UTC04 - Boundary: id = null → throw exception")
        void getProduct_IdNull() {
            assertThrows(Exception.class, () -> service.getProduct(null));
        }
    }


    // ======================================================================
    // 🔵 TEST listProducts()
    // ======================================================================
    @Nested
    @DisplayName("listProducts() Tests")
    class ListProductsTests {

        @Test
        @DisplayName("UTC01 - Normal: return List<Product> size = 2")
        void listProducts_Normal() {
            Product p1 = new Product(); p1.setId(1L); p1.setName("p1");
            Product p2 = new Product(); p2.setId(2L); p2.setName("p2");

            when(productRepo.findAll()).thenReturn(List.of(p1, p2));

            List<Product> result = service.listProducts();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(productRepo, times(1)).findAll();
        }

        @Test
        @DisplayName("UTC02 - Boundary: return empty list")
        void listProducts_Empty() {
            when(productRepo.findAll()).thenReturn(List.of());

            List<Product> result = service.listProducts();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(productRepo, times(1)).findAll();
        }
    }
}
