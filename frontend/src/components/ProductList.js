import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Form, Button, Spinner, Alert } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';

const ProductList = ({ categories }) => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [sortOrder, setSortOrder] = useState('asc');

  useEffect(() => {
    fetchProducts();
  }, [searchTerm, selectedCategory, sortBy, sortOrder]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      setError(null);
      
      let url = '/api/products?';
      const params = new URLSearchParams();
      
      if (searchTerm) params.append('search', searchTerm);
      if (selectedCategory) params.append('categoryId', selectedCategory);
      params.append('sortBy', sortBy);
      params.append('sortDir', sortOrder);
      
      url += params.toString();
      
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error('Failed to fetch products');
      }
      
      const data = await response.json();
      setProducts(data);
    } catch (err) {
      setError(err.message);
      toast.error('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchProducts();
  };

  const clearFilters = () => {
    setSearchTerm('');
    setSelectedCategory('');
    setSortBy('name');
    setSortOrder('asc');
  };

  if (loading) {
    return (
      <Container className="text-center py-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading products...</span>
        </Spinner>
      </Container>
    );
  }

  return (
    <Container fluid>
      {/* Header */}
      <div className="page-header">
        <Container>
          <h1>Our Products</h1>
          <p>Discover amazing products from our catalog</p>
        </Container>
      </div>

      {/* Search and Filters */}
      <Container>
        <div className="search-container">
          <Form onSubmit={handleSearch}>
            <Row className="g-3">
              <Col md={4}>
                <Form.Group>
                  <Form.Label>Search Products</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="Enter product name or description..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group>
                  <Form.Label>Category</Form.Label>
                  <Form.Select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                  >
                    <option value="">All Categories</option>
                    {categories.map(category => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={2}>
                <Form.Group>
                  <Form.Label>Sort By</Form.Label>
                  <Form.Select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                  >
                    <option value="name">Name</option>
                    <option value="price">Price</option>
                    <option value="createdAt">Date Added</option>
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={2}>
                <Form.Group>
                  <Form.Label>Order</Form.Label>
                  <Form.Select
                    value={sortOrder}
                    onChange={(e) => setSortOrder(e.target.value)}
                  >
                    <option value="asc">Ascending</option>
                    <option value="desc">Descending</option>
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={1} className="d-flex align-items-end">
                <Button type="submit" variant="primary" className="w-100">
                  Search
                </Button>
              </Col>
            </Row>
            <Row className="mt-2">
              <Col>
                <Button variant="outline-secondary" onClick={clearFilters}>
                  Clear Filters
                </Button>
              </Col>
            </Row>
          </Form>
        </div>

        {/* Error Message */}
        {error && (
          <Alert variant="danger">
            {error}
          </Alert>
        )}

        {/* Products Grid */}
        {products.length === 0 ? (
          <div className="text-center py-5">
            <h4>No products found</h4>
            <p>Try adjusting your search criteria or browse all products.</p>
          </div>
        ) : (
          <Row>
            {products.map(product => (
              <Col key={product.id} lg={3} md={4} sm={6} className="mb-4">
                <Card className="product-card h-100">
                  {product.imageUrl ? (
                    <Card.Img
                      variant="top"
                      src={product.imageUrl}
                      alt={product.name}
                      className="product-image"
                    />
                  ) : (
                    <div className="product-image d-flex align-items-center justify-content-center bg-light">
                      <span className="text-muted">No Image</span>
                    </div>
                  )}
                  <Card.Body className="d-flex flex-column">
                    <div className="category-badge">
                      <span className="badge bg-secondary">{product.categoryName}</span>
                    </div>
                    <Card.Title className="h6">{product.name}</Card.Title>
                    <Card.Text className="text-muted small flex-grow-1">
                      {product.description?.substring(0, 100)}
                      {product.description?.length > 100 && '...'}
                    </Card.Text>
                    <div className="mt-auto">
                      <div className="price">${product.price}</div>
                      <div className="stock-info">
                        Stock: {product.stockQuantity} units
                      </div>
                      <div className="mt-2">
                        <Link
                          to={`/product/${product.id}`}
                          className="btn btn-primary btn-sm w-100"
                        >
                          View Details
                        </Link>
                      </div>
                    </div>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </Container>
    </Container>
  );
};

export default ProductList;
